package com.khanh.fooddelivery.catalog_service.outbox;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxEventClaimService {
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxProperties properties;
    private final Clock clock;

    @Transactional
    public List<OutboxEvent> claimNextBatch() {
        Instant now = clock.instant();
        List<OutboxEvent> events =
                outboxEventRepository.lockNextPublishable(
                        now, properties.getPublisher().getBatchSize());
        Instant leaseExpiresAt = now.plusMillis(properties.getPublisher().getProcessingLeaseMs());
        for (OutboxEvent event : events) {
            event.setStatus(OutboxStatus.PROCESSING);
            event.setNextRetryAt(leaseExpiresAt);
            event.setUpdatedAt(now);
        }
        return events;
    }
}
