package com.khanh.fooddelivery.delivery_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;

import com.khanh.fooddelivery.delivery_service.client.PaymentServiceClient;
import com.khanh.fooddelivery.delivery_service.client.RestaurantServiceClient;
import com.khanh.fooddelivery.delivery_service.client.dto.response.FinancialFactsResponse;
import com.khanh.fooddelivery.delivery_service.client.dto.response.RestaurantBranchOrderingContextResponse;
import com.khanh.fooddelivery.delivery_service.common.response.ApiResponse;
import com.khanh.fooddelivery.delivery_service.event.OrderConfirmedEvent;
import com.khanh.fooddelivery.delivery_service.model.Delivery;
import com.khanh.fooddelivery.delivery_service.model.DeliveryStatus;
import com.khanh.fooddelivery.delivery_service.repository.DeliveryRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DeliveryCreationServiceImplTests {

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID BRANCH_ID = UUID.randomUUID();

    @Mock
    private DeliveryRepository deliveries;
    @Mock
    private RestaurantServiceClient restaurants;
    @Mock
    private PaymentServiceClient payment;

    private DeliveryCreationServiceImpl creation;

    @BeforeEach
    void setUp() {
        creation = new DeliveryCreationServiceImpl(deliveries, restaurants, payment);
        ReflectionTestUtils.setField(creation, "internalApiKey", "internal-test-key");
        ReflectionTestUtils.setField(creation, "dispatchMaxDuration", java.time.Duration.ofMinutes(10));
        lenient().when(restaurants.getOrderingContext(any(), anyString(), eq(BRANCH_ID)))
                .thenReturn(ApiResponse.success("context", new RestaurantBranchOrderingContextResponse(
                        UUID.randomUUID(), "Restaurant", true, BRANCH_ID, "Branch", true, true,
                        "120 Nguyen Trai", "Ward 1", "District 1", "Hanoi",
                        new BigDecimal("21.0200"), new BigDecimal("105.8200"))));
        lenient().when(payment.facts(anyString(), eq(ORDER_ID))).thenReturn(ApiResponse.success(
                "facts", new FinancialFactsResponse(
                        UUID.randomUUID(), ORDER_ID, "COD", "PENDING",
                        "VND", new BigDecimal("50000"), new BigDecimal("25000"),
                        new BigDecimal("75000"), new BigDecimal("50000"), new BigDecimal("75000"),
                        new BigDecimal("25000"), new BigDecimal("15000"), new BigDecimal("7500"),
                        new BigDecimal("17500"), new BigDecimal("35000"), new BigDecimal("22500"),
                        UUID.randomUUID(), 1, false, false)));
    }

    @Test
    void ensureCreatesOnlyMatchingDeliveryAndDoesNotQueryDrivers() {
        when(deliveries.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.empty());
        when(deliveries.save(any(Delivery.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Delivery delivery = creation.ensureMatching(request());

        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.MATCHING);
        assertThat(delivery.getMatchingStartedAt()).isNotNull();
        assertThat(delivery.getNextDispatchAt()).isNotNull();
        assertThat(delivery.getDispatchDeadlineAt()).isAfter(delivery.getMatchingStartedAt());
        assertThat(delivery.getDispatchAttemptCount()).isZero();
    }

    @Test
    void ensureIsIdempotentByOrderId() {
        Delivery existing = new Delivery();
        existing.setId(UUID.randomUUID());
        existing.setOrderId(ORDER_ID);
        existing.setStatus(DeliveryStatus.MATCHING);
        when(deliveries.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(existing));

        Delivery result = creation.ensureMatching(request());

        assertThat(result).isSameAs(existing);
    }

    @Test
    void duplicateEventDoesNotResetAssignedDelivery() {
        Delivery existing = new Delivery();
        existing.setId(UUID.randomUUID());
        existing.setOrderId(ORDER_ID);
        existing.setStatus(DeliveryStatus.ASSIGNED);
        existing.setDriverId(UUID.randomUUID());
        when(deliveries.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(existing));

        Delivery result = creation.ensureMatching(request());

        assertThat(result.getStatus()).isEqualTo(DeliveryStatus.ASSIGNED);
        assertThat(result.getDriverId()).isNotNull();
        verifyNoInteractions(restaurants, payment);
    }

    private OrderConfirmedEvent.Payload request() {
        return new OrderConfirmedEvent.Payload(
                ORDER_ID, UUID.randomUUID(), BRANCH_ID, UUID.randomUUID(),
                "Restaurant", "Branch", "Company", "97 Man Thien",
                new BigDecimal("21.0300"), new BigDecimal("105.8500"));
    }
}
