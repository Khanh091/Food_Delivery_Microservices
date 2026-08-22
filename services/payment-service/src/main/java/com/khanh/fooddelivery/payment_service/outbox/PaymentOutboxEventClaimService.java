package com.khanh.fooddelivery.payment_service.outbox;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentOutboxEventClaimService {

    private final PaymentOutboxEventRepository repository;
    private final PaymentOutboxProperties properties;
    private final Clock clock;

    @Transactional
    public List<PaymentOutboxEvent> claimNextBatch() {
        Instant now = clock.instant();
        List<PaymentOutboxEvent> events = repository.lockNextPublishable(
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
