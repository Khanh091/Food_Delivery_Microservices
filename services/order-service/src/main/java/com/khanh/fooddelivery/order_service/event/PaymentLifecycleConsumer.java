package com.khanh.fooddelivery.order_service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khanh.fooddelivery.order_service.service.OrderService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentLifecycleConsumer {

    private final ObjectMapper objectMapper;
    private final OrderService orders;

    @KafkaListener(
            topics = "${app.kafka.payment-lifecycle-topic:payment.lifecycle.v1}",
            groupId = "${app.kafka.payment-lifecycle-consumer-group:order-service-payment-lifecycle}",
            containerFactory = "paymentLifecycleKafkaListenerContainerFactory"
    )
    public void consume(String message) {
        PaymentLifecycleEvent event = null;
        try {
            event = objectMapper.readValue(message, PaymentLifecycleEvent.class);
            validate(event);
            apply(event);
            log.info("Payment lifecycle event handled eventId={} eventType={} orderId={}",
                    event.eventId(), event.eventType(), event.payload().orderId());
        } catch (Exception exception) {
            log.error("Payment lifecycle event processing failed eventId={} eventType={} orderId={}",
                    eventId(event), eventType(event), orderId(event), exception);
            throw new IllegalStateException("Payment lifecycle event processing failed", exception);
        }
    }

    private void apply(PaymentLifecycleEvent event) {
        UUID orderId = event.payload().orderId();
        switch (event.eventType()) {
            case PaymentLifecycleEvent.PAYMENT_SUCCEEDED -> orders.paymentSucceeded(orderId);
            case PaymentLifecycleEvent.PAYMENT_FAILED -> orders.paymentFailed(orderId);
            case PaymentLifecycleEvent.PAYMENT_COLLECTED -> orders.paymentCollected(orderId);
            default -> throw new IllegalArgumentException("Unsupported payment lifecycle event");
        }
    }

    private void validate(PaymentLifecycleEvent event) {
        if (event == null
                || event.eventId() == null
                || event.occurredAt() == null
                || event.aggregateId() == null
                || event.version() != PaymentLifecycleEvent.VERSION
                || event.payload() == null
                || event.payload().paymentId() == null
                || event.payload().orderId() == null
                || !event.aggregateId().equals(event.payload().paymentId())
                || !expectedStatus(event.eventType()).equals(event.payload().paymentStatus())) {
            throw new IllegalArgumentException("Invalid payment lifecycle event");
        }
    }

    private String expectedStatus(String eventType) {
        return switch (eventType) {
            case PaymentLifecycleEvent.PAYMENT_SUCCEEDED -> "PAID";
            case PaymentLifecycleEvent.PAYMENT_FAILED -> "FAILED";
            case PaymentLifecycleEvent.PAYMENT_COLLECTED -> "COLLECTED";
            default -> throw new IllegalArgumentException("Unsupported payment lifecycle event");
        };
    }

    private UUID eventId(PaymentLifecycleEvent event) {
        return event == null ? null : event.eventId();
    }

    private String eventType(PaymentLifecycleEvent event) {
        return event == null ? null : event.eventType();
    }

    private UUID orderId(PaymentLifecycleEvent event) {
        return event == null || event.payload() == null ? null : event.payload().orderId();
    }
}
