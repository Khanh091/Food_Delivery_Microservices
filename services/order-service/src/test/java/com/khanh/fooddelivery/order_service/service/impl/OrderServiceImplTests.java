package com.khanh.fooddelivery.order_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.order_service.client.CartServiceClient;
import com.khanh.fooddelivery.order_service.client.PaymentServiceClient;
import com.khanh.fooddelivery.order_service.client.dto.response.PaymentResponse;
import com.khanh.fooddelivery.order_service.common.response.ApiResponse;
import com.khanh.fooddelivery.order_service.entity.Order;
import com.khanh.fooddelivery.order_service.enums.OrderStatus;
import com.khanh.fooddelivery.order_service.enums.PaymentMethod;
import com.khanh.fooddelivery.order_service.enums.PaymentStatus;
import com.khanh.fooddelivery.order_service.mapper.OrderMapper;
import com.khanh.fooddelivery.order_service.outbox.OrderOutboxService;
import com.khanh.fooddelivery.order_service.repository.OrderRepository;
import com.khanh.fooddelivery.order_service.security.CurrentBearerTokenProvider;
import com.khanh.fooddelivery.order_service.security.CurrentUserProvider;
import com.khanh.fooddelivery.order_service.service.CheckoutPreviewService;
import com.khanh.fooddelivery.order_service.service.OrderSnapshotFactory;
import com.khanh.fooddelivery.order_service.service.RestaurantAuthorizationService;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTests {

    private static final UUID ORDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID RESTAURANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID BRANCH_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

    @Mock private OrderRepository orders;
    @Mock private CheckoutPreviewService checkoutPreviewService;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private CurrentBearerTokenProvider bearerTokenProvider;
    @Mock private CartServiceClient cartClient;
    @Mock private OrderMapper orderMapper;
    @Mock private OrderSnapshotFactory orderSnapshotFactory;
    @Mock private RestaurantAuthorizationService restaurantAuthorization;
    @Mock private PaymentServiceClient paymentClient;
    @Mock private OrderOutboxService orderOutbox;

    private OrderServiceImpl service;
    private Order order;

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl(
                orders,
                checkoutPreviewService,
                currentUserProvider,
                bearerTokenProvider,
                cartClient,
                orderMapper,
                orderSnapshotFactory,
                restaurantAuthorization,
                paymentClient,
                orderOutbox
        );
        ReflectionTestUtils.setField(service, "internalApiKey", "internal-test-key");

        order = new Order();
        order.setId(ORDER_ID);
        order.setRestaurantId(RESTAURANT_ID);
        order.setBranchId(BRANCH_ID);
        order.setCustomerId(CUSTOMER_ID);
        order.setRestaurantName("Restaurant");
        order.setBranchName("Branch");
        order.setStatus(OrderStatus.PENDING_RESTAURANT);
        order.setAddressDisplayLabel("Nhà");
        order.setFormattedAddress("97 Man Thien, Thu Duc, Ho Chi Minh City");
        order.setLatitude(new BigDecimal("10.8000"));
        order.setLongitude(new BigDecimal("106.7500"));
        when(orders.findWithItemsById(ORDER_ID)).thenReturn(Optional.of(order));
    }

    @Test
    void acceptConfirmsOrderAndPersistsOrderConfirmedOutboxWithoutDeliveryCall() {
        service.accept(null, ORDER_ID);

        verify(orders).saveAndFlush(order);
        verify(orderOutbox).publishOrderConfirmed(order);
        verify(bearerTokenProvider, never()).getBearerToken();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(OrderServiceImpl.class.isAnnotationPresent(Transactional.class)).isTrue();
    }

    @Test
    void terminalMatchingFailureCancelsPendingCodPayment() {
        order.setPaymentId(UUID.randomUUID());
        order.setPaymentMethod(PaymentMethod.COD);
        order.setPaymentStatus(PaymentStatus.PENDING);
        when(paymentClient.cancel(eq("internal-test-key"), eq(ORDER_ID)))
                .thenReturn(ApiResponse.success("cancelled", payment(PaymentStatus.CANCELLED)));
        order.setStatus(OrderStatus.CONFIRMED);

        service.matchingFailed(ORDER_ID);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
        verify(paymentClient).cancel("internal-test-key", ORDER_ID);
    }

    @Test
    void terminalMatchingFailureRefundsPaidOnlinePayment() {
        order.setPaymentId(UUID.randomUUID());
        order.setPaymentMethod(PaymentMethod.ONLINE);
        order.setPaymentStatus(PaymentStatus.PAID);
        when(paymentClient.refund(eq("internal-test-key"), eq(ORDER_ID)))
                .thenReturn(ApiResponse.success("refunded", payment(PaymentStatus.REFUNDED)));
        order.setStatus(OrderStatus.CONFIRMED);

        service.matchingFailed(ORDER_ID);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(paymentClient).refund("internal-test-key", ORDER_ID);
    }

    @Test
    void collectedPaymentProjectionCanBeUpdatedIdempotently() {
        order.setPaymentStatus(PaymentStatus.PENDING);

        service.paymentCollected(ORDER_ID);
        service.paymentCollected(ORDER_ID);

        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.COLLECTED);
    }

    @Test
    void paymentSucceededMovesPendingPaymentOrderAndDuplicateIsNoOp() {
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setPaymentStatus(PaymentStatus.PENDING);

        service.paymentSucceeded(ORDER_ID);
        service.paymentSucceeded(ORDER_ID);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_RESTAURANT);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void paymentSucceededAfterOrderAdvancedIsNoOp() {
        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaymentStatus(PaymentStatus.PAID);

        service.paymentSucceeded(ORDER_ID);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void paymentFailedUpdatesPendingPaymentProjection() {
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setPaymentStatus(PaymentStatus.PROCESSING);

        service.paymentFailed(ORDER_ID);

        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void paymentEventCannotApplyImpossibleTransition() {
        order.setStatus(OrderStatus.PENDING_RESTAURANT);
        order.setPaymentStatus(PaymentStatus.PENDING);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.paymentSucceeded(ORDER_ID))
                .isInstanceOf(com.khanh.fooddelivery.order_service.exception.AppException.class);
    }

    private PaymentResponse payment(PaymentStatus status) {
        return new PaymentResponse(
                UUID.randomUUID(), ORDER_ID, order.getPaymentMethod(), status,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
    }
}
