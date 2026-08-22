package com.khanh.fooddelivery.payment_service.outbox;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentOutboxEventPublisher {

    private final PaymentOutboxEventClaimService claims;
    private final PaymentOutboxEventStateService states;
    private final PaymentOutboxProperties properties;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${app.outbox.publisher.fixed-delay-ms:1000}")
    public void publishPendingEvents() {
        if (!properties.getPublisher().isEnabled()) {
            return;
        }
        claims.claimNextBatch().forEach(this::publish);
    }

    private void publish(PaymentOutboxEvent event) {
        try {
            kafkaTemplate.send(
                    properties.getTopic(),
                    event.getAggregateId().toString(),
                    event.getPayload().toString()
            ).get(properties.getPublisher().getSendTimeoutMs(), TimeUnit.MILLISECONDS);
            states.markPublished(event.getId(), event.getClaimToken());
            log.info("Payment outbox event published eventId={} eventType={} aggregateId={}",
                    event.getId(), event.getEventType(), event.getAggregateId());
        } catch (Exception exception) {
            states.markFailed(event.getId(), event.getClaimToken(), exception);
            log.warn("Payment outbox publish failed eventId={} eventType={} aggregateId={}",
                    event.getId(), event.getEventType(), event.getAggregateId());
        }
    }
}
