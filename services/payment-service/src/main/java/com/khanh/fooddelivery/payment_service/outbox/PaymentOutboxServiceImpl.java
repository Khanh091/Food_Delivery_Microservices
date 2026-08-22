package com.khanh.fooddelivery.payment_service.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khanh.fooddelivery.payment_service.entity.Payment;
import com.khanh.fooddelivery.payment_service.event.PaymentLifecycleEvent;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentOutboxServiceImpl implements PaymentOutboxService {

    private static final String AGGREGATE_TYPE = "PAYMENT";

    private final PaymentOutboxEventRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishPaymentSucceeded(Payment payment) {
        publish(payment, PaymentLifecycleEvent.PAYMENT_SUCCEEDED);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishPaymentFailed(Payment payment) {
        publish(payment, PaymentLifecycleEvent.PAYMENT_FAILED);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishPaymentCollected(Payment payment) {
        publish(payment, PaymentLifecycleEvent.PAYMENT_COLLECTED);
    }

    private void publish(Payment payment, String eventType) {
        if (repository.existsByAggregateTypeAndAggregateIdAndEventType(
                AGGREGATE_TYPE, payment.getId(), eventType)) {
            return;
        }

        Instant occurredAt = clock.instant();
        UUID eventId = UUID.randomUUID();
        PaymentLifecycleEvent event = new PaymentLifecycleEvent(
                eventId,
                eventType,
                occurredAt,
                payment.getId(),
                PaymentLifecycleEvent.VERSION,
                new PaymentLifecycleEvent.Payload(
                        payment.getId(),
                        payment.getOrderId(),
                        payment.getStatus().name()
                )
        );

        PaymentOutboxEvent outbox = new PaymentOutboxEvent();
        outbox.setId(eventId);
        outbox.setAggregateType(AGGREGATE_TYPE);
        outbox.setAggregateId(payment.getId());
        outbox.setEventType(eventType);
        outbox.setPayload(objectMapper.valueToTree(event));
        outbox.setCreatedAt(occurredAt);
        outbox.setAttemptCount(0);
        repository.save(outbox);
    }
}
