package com.khanh.fooddelivery.restaurant_service.outbox;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxEventClaimService claims;
    private final OutboxEventStateService states;
    private final OutboxProperties properties;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${app.outbox.publisher.fixed-delay-ms:1000}")
    public void publishPendingEvents() {
        if (!properties.getPublisher().isEnabled()) {
            return;
        }

        claims.claimNextBatch().forEach(this::publish);
    }

    private void publish(OutboxEvent event) {
        try {
            kafkaTemplate.send(
                    properties.getTopic(),
                    event.getAggregateId().toString(),
                    event.getPayload().toString()
            ).get(
                    properties.getPublisher().getSendTimeoutMs(),
                    TimeUnit.MILLISECONDS
            );

            states.markPublished(event.getId());
        } catch (Exception exception) {
            states.markFailed(event.getId(), exception);

            log.warn(
                    "Outbox publish failed eventId={} eventType={} aggregateId={}",
                    event.getId(),
                    event.getEventType(),
                    event.getAggregateId()
            );
        }
    }
}