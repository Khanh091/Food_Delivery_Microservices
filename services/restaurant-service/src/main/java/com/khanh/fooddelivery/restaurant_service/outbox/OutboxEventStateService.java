package com.khanh.fooddelivery.restaurant_service.outbox;

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

    private final OutboxEventRepository repository;
    private final OutboxProperties properties;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPublished(UUID id) {
        OutboxEvent event = required(id);
        Instant now = clock.instant();

        event.setStatus(OutboxStatus.PUBLISHED);
        event.setPublishedAt(now);
        event.setNextRetryAt(null);
        event.setLastError(null);
        event.setUpdatedAt(now);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID id, Throwable error) {
        OutboxEvent event = required(id);

        if (event.getStatus() == OutboxStatus.PUBLISHED) {
            return;
        }

        Instant now = clock.instant();
        int retries = event.getRetryCount() + 1;

        event.setRetryCount(retries);
        event.setLastError(
                error.getClass().getSimpleName()
                        + ": "
                        + String.valueOf(error.getMessage())
        );
        event.setUpdatedAt(now);

        if (retries >= properties.getPublisher().getMaxRetries()) {
            event.setStatus(OutboxStatus.FAILED);
            event.setNextRetryAt(null);
        } else {
            event.setStatus(OutboxStatus.PENDING);

            long delay = Math.min(
                    properties.getPublisher().getInitialRetryDelayMs()
                            * (1L << Math.min(retries - 1, 20)),
                    properties.getPublisher().getMaxRetryDelayMs()
            );

            event.setNextRetryAt(now.plusMillis(delay));
        }
    }

    private OutboxEvent required(UUID id) {
        return repository.findById(id)
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Outbox event not found: " + id
                        )
                );
    }
}