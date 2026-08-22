package com.khanh.fooddelivery.order_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.order_service.client.PaymentServiceClient;
import com.khanh.fooddelivery.order_service.client.dto.response.PaymentResponse;
import com.khanh.fooddelivery.order_service.common.response.ApiResponse;
import com.khanh.fooddelivery.order_service.dto.response.CheckoutPreviewResponse;
import com.khanh.fooddelivery.order_service.dto.response.OrderResponse;
import com.khanh.fooddelivery.order_service.entity.Order;
import com.khanh.fooddelivery.order_service.enums.PaymentMethod;
import com.khanh.fooddelivery.order_service.enums.PaymentStatus;
import com.khanh.fooddelivery.order_service.exception.AppException;
import com.khanh.fooddelivery.order_service.exception.ErrorCode;
import com.khanh.fooddelivery.order_service.mapper.OrderMapper;
import com.khanh.fooddelivery.order_service.repository.OrderRepository;
import com.khanh.fooddelivery.order_service.service.OrderSnapshotFactory;
import feign.FeignException;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class OrderCreationTransactionServiceTests {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID RESTAURANT_ID = UUID.randomUUID();
    private static final UUID BRANCH_ID = UUID.randomUUID();

    @Mock private OrderRepository orders;
    @Mock private OrderSnapshotFactory snapshots;
    @Mock private PaymentServiceClient payments;
    @Mock private OrderMapper mapper;

    private OrderCreationTransactionService service;
    private CheckoutPreviewResponse preview;
    private Order order;

    @BeforeEach
    void setUp() {
        service = new OrderCreationTransactionService(orders, snapshots, payments, mapper);
        ReflectionTestUtils.setField(service, "internalApiKey", "internal-test-key");
        preview = mock(CheckoutPreviewResponse.class);
        order = order();
    }

    @ParameterizedTest
    @EnumSource(PaymentMethod.class)
    void successfulPlacementPersistsOrderAndPaymentProjection(PaymentMethod method) {
        order.setPaymentMethod(method);
        PaymentResponse payment = payment(method);
        when(snapshots.create(ORDER_ID, CUSTOMER_ID, preview, method)).thenReturn(order);
        when(mapper.toResponse(order)).thenReturn(mock(OrderResponse.class));
        when(payments.create(eq("internal-test-key"), any()))
                .thenReturn(ApiResponse.success("created", payment));

        OrderResponse response = service.create(ORDER_ID, CUSTOMER_ID, preview, method);

        assertThat(response).isNotNull();
        assertThat(order.getPaymentId()).isEqualTo(payment.id());
        assertThat(order.getPaymentStatus()).isEqualTo(payment.status());
        assertThat(order.getFeePolicyId()).isEqualTo(payment.feePolicyId());
        verify(orders).saveAndFlush(order);
        verify(orders, never()).save(order);
    }

    @Test
    void paymentFailureRollsBackTheTransactionalPlacementBoundary() {
        when(snapshots.create(ORDER_ID, CUSTOMER_ID, preview, PaymentMethod.ONLINE)).thenReturn(order);
        FeignException failure = mock(FeignException.class);
        when(payments.create(eq("internal-test-key"), any())).thenThrow(failure);

        assertThatThrownBy(() -> service.create(ORDER_ID, CUSTOMER_ID, preview, PaymentMethod.ONLINE))
                .isInstanceOf(AppException.class)
                .extracting(exception -> ((AppException) exception).getErrorCode())
                .isEqualTo(ErrorCode.PAYMENT_SERVICE_UNAVAILABLE);
        verify(orders).saveAndFlush(order);
        verify(mapper, never()).toResponse(any());
    }

    @Test
    void placementMethodOwnsTheLocalTransaction() throws Exception {
        assertThat(OrderCreationTransactionService.class
                .getDeclaredMethod("create", UUID.class, UUID.class, CheckoutPreviewResponse.class, PaymentMethod.class)
                .isAnnotationPresent(Transactional.class)).isTrue();
    }

    private Order order() {
        Order value = new Order();
        value.setId(ORDER_ID);
        value.setCustomerId(CUSTOMER_ID);
        value.setRestaurantId(RESTAURANT_ID);
        value.setBranchId(BRANCH_ID);
        value.setPaymentMethod(PaymentMethod.COD);
        value.setItemsSubtotal(BigDecimal.valueOf(100));
        value.setDeliveryFee(BigDecimal.valueOf(15000));
        value.setDiscountAmount(BigDecimal.ZERO);
        value.setTotalAmount(BigDecimal.valueOf(15100));
        value.setCurrency("VND");
        return value;
    }

    private PaymentResponse payment(PaymentMethod method) {
        return new PaymentResponse(
                UUID.randomUUID(), ORDER_ID, method, PaymentStatus.PENDING,
                BigDecimal.valueOf(15100), "VND", null, null, null,
                null, null, null, UUID.randomUUID(), 1,
                BigDecimal.TEN, BigDecimal.valueOf(90), BigDecimal.TEN,
                BigDecimal.valueOf(90), BigDecimal.TEN);
    }
}
