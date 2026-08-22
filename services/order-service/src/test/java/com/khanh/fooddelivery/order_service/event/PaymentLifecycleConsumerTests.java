package com.khanh.fooddelivery.order_service.event;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.khanh.fooddelivery.order_service.service.OrderService;
import java.time.Instant;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentLifecycleConsumerTests {

    @Mock
    private OrderService orders;

    private ObjectMapper objectMapper;
    private PaymentLifecycleConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        consumer = new PaymentLifecycleConsumer(objectMapper, orders);
    }

    @Test
    void validPaymentSucceededEventDelegatesToOrderApplicationService() throws Exception {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentLifecycleEvent event = event(
                paymentId,
                orderId,
                PaymentLifecycleEvent.PAYMENT_SUCCEEDED,
                "PAID"
        );

        consumer.consume(objectMapper.writeValueAsString(event));

        verify(orders).paymentSucceeded(orderId);
    }

    @Test
    void collectedEventDelegatesOnlyProjectionFact() throws Exception {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentLifecycleEvent event = event(
                paymentId,
                orderId,
                PaymentLifecycleEvent.PAYMENT_COLLECTED,
                "COLLECTED"
        );

        consumer.consume(objectMapper.writeValueAsString(event));

        verify(orders).paymentCollected(orderId);
    }

    @Test
    void invalidEventIsRejectedForRetryOrDlt() throws Exception {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentLifecycleEvent event = event(
                paymentId,
                orderId,
                PaymentLifecycleEvent.PAYMENT_SUCCEEDED,
                "FAILED"
        );

        Assertions.assertThatThrownBy(() -> consumer.consume(objectMapper.writeValueAsString(event)))
                .isInstanceOf(IllegalStateException.class);
        verify(orders, never()).paymentSucceeded(orderId);
        verify(orders, never()).paymentFailed(orderId);
        verify(orders, never()).paymentCollected(orderId);
    }

    private PaymentLifecycleEvent event(UUID paymentId, UUID orderId, String eventType, String status) {
        return new PaymentLifecycleEvent(
                UUID.randomUUID(),
                eventType,
                Instant.parse("2026-08-23T10:15:30Z"),
                paymentId,
                PaymentLifecycleEvent.VERSION,
                new PaymentLifecycleEvent.Payload(paymentId, orderId, status)
        );
    }
}
