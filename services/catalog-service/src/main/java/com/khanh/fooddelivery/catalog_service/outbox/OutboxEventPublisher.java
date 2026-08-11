package com.khanh.fooddelivery.catalog_service.outbox;

import java.util.List;
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
    private final OutboxEventClaimService claimService;
    private final OutboxEventStateService stateService;
    private final OutboxProperties properties;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${app.outbox.publisher.fixed-delay-ms:1000}")
    public void publishPendingEvents() {
        if (!properties.getPublisher().isEnabled()) {
            return;
        }
        List<OutboxEvent> events = claimService.claimNextBatch();
        for (OutboxEvent event : events) {
            publish(event);
        }
    }

    private void publish(OutboxEvent event) {
        try {
            kafkaTemplate
                    .send(
                            properties.getTopic(),
                            event.getAggregateId().toString(),
                            event.getPayload().toString())
                    .get(properties.getPublisher().getSendTimeoutMs(), TimeUnit.MILLISECONDS);
            stateService.markPublished(event.getId());
            log.info(
                    "Published outbox event eventId={} eventType={} aggregateId={}",
                    event.getId(),
                    event.getEventType(),
                    event.getAggregateId());
        } catch (Exception exception) {
            stateService.markFailed(event.getId(), exception);
            log.warn(
                    "Outbox publish failed eventId={} eventType={} aggregateId={} retryCount={}",
                    event.getId(),
                    event.getEventType(),
                    event.getAggregateId(),
                    event.getRetryCount() + 1);
        }
    }
}
