package com.khanh.fooddelivery.order_service.outbox;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
        Instant leaseUntil = now.plusMillis(properties.getPublisher().getProcessingLeaseMs());
        events.forEach(event -> {
            event.setClaimedAt(leaseUntil);
            event.setNextAttemptAt(leaseUntil);
            event.setClaimToken(UUID.randomUUID());
        });
        return events;
    }
}
