package com.khanh.fooddelivery.catalog_service.outbox;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxEventStateService {
    private static final int MAX_ERROR_LENGTH = 1000;

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxProperties properties;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPublished(UUID eventId) {
        OutboxEvent event = requiredEvent(eventId);
        Instant now = clock.instant();
        event.setStatus(OutboxStatus.PUBLISHED);
        event.setPublishedAt(now);
        event.setNextRetryAt(null);
        event.setLastError(null);
        event.setUpdatedAt(now);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID eventId, Throwable exception) {
        OutboxEvent event = requiredEvent(eventId);
        if (event.getStatus() == OutboxStatus.PUBLISHED) {
            return;
        }

        Instant now = clock.instant();
        int retryCount = event.getRetryCount() + 1;
        event.setRetryCount(retryCount);
        event.setLastError(sanitize(exception));
        event.setUpdatedAt(now);
        if (retryCount >= properties.getPublisher().getMaxRetries()) {
            event.setStatus(OutboxStatus.FAILED);
            event.setNextRetryAt(null);
            return;
        }

        event.setStatus(OutboxStatus.PENDING);
        event.setNextRetryAt(now.plusMillis(retryDelayMillis(retryCount)));
    }

    private OutboxEvent requiredEvent(UUID eventId) {
        return outboxEventRepository
                .findById(eventId)
                .orElseThrow(() -> new IllegalStateException("Outbox event not found: " + eventId));
    }

    private long retryDelayMillis(int retryCount) {
        long initialDelay = properties.getPublisher().getInitialRetryDelayMs();
        long maximumDelay = properties.getPublisher().getMaxRetryDelayMs();
        int exponent = Math.min(retryCount - 1, 20);
        long multiplier = 1L << exponent;
        return Math.min(initialDelay * multiplier, maximumDelay);
    }

    private String sanitize(Throwable exception) {
        String message = exception.getClass().getSimpleName() + ": " + exception.getMessage();
        return message.length() <= MAX_ERROR_LENGTH
                ? message
                : message.substring(0, MAX_ERROR_LENGTH);
    }
}
