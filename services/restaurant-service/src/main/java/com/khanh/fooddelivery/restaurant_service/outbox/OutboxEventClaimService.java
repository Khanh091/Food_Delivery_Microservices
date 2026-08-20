package com.khanh.fooddelivery.restaurant_service.outbox;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxEventClaimService {

    private final OutboxEventRepository repository;
    private final OutboxProperties properties;
    private final Clock clock;

    @Transactional
    public List<OutboxEvent> claimNextBatch() {
        Instant now = clock.instant();
        List<OutboxEvent> events = repository.lockNextPublishable(
                now,
                properties.getPublisher().getBatchSize()
        );
        Instant lease = now.plusMillis(
                properties.getPublisher().getProcessingLeaseMs()
        );

        events.forEach(event -> {
            event.setStatus(OutboxStatus.PROCESSING);
            event.setNextRetryAt(lease);
            event.setUpdatedAt(now);
        });

        return events;
    }
}