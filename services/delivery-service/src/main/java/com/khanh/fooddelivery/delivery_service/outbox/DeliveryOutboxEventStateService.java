package com.khanh.fooddelivery.delivery_service.outbox;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeliveryOutboxEventStateService {

    private final DeliveryOutboxEventRepository repository;
    private final DeliveryOutboxProperties properties;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPublished(UUID id, UUID claimToken) {
        DeliveryOutboxEvent event = repository.findById(id).orElse(null);
        if (event == null || event.getPublishedAt() != null || !claimMatches(event, claimToken)) {
            return;
        }
        event.setPublishedAt(clock.instant());
        event.setClaimedAt(null);
        event.setClaimToken(null);
        event.setNextAttemptAt(null);
        event.setLastError(null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID id, UUID claimToken, Throwable failure) {
        DeliveryOutboxEvent event = repository.findById(id).orElse(null);
        if (event == null || event.getPublishedAt() != null || !claimMatches(event, claimToken)) {
            return;
        }
        Instant now = clock.instant();
        event.setAttemptCount(event.getAttemptCount() + 1);
        String message = failure.getClass().getSimpleName() + ": " + String.valueOf(failure.getMessage());
        event.setLastError(message.length() > 1000 ? message.substring(0, 1000) : message);
        event.setClaimedAt(null);
        event.setClaimToken(null);
        event.setNextAttemptAt(now.plusMillis(properties.getPublisher().getRetryDelayMs()));
    }

    private boolean claimMatches(DeliveryOutboxEvent event, UUID claimToken) {
        return claimToken != null && claimToken.equals(event.getClaimToken());
    }
}
