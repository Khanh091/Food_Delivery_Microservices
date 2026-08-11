package com.khanh.fooddelivery.catalog_service.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class OutboxEventStateServiceTests {
    @Test
    void failedSendIncrementsRetryCountAndSchedulesBackoff() {
        OutboxEventRepository repository = Mockito.mock(OutboxEventRepository.class);
        OutboxProperties properties = new OutboxProperties();
        properties.getPublisher().setInitialRetryDelayMs(5000);
        OutboxEvent event = event();
        when(repository.findById(event.getId())).thenReturn(Optional.of(event));
        Instant now = Instant.parse("2026-08-11T10:00:00Z");
        OutboxEventStateService service =
                new OutboxEventStateService(
                        repository, properties, Clock.fixed(now, ZoneOffset.UTC));

        service.markFailed(event.getId(), new IllegalStateException("broker unavailable"));

        assertEquals(OutboxStatus.PENDING, event.getStatus());
        assertEquals(1, event.getRetryCount());
        assertEquals(now.plusSeconds(5), event.getNextRetryAt());
    }

    @Test
    void maximumRetryMarksEventFailed() {
        OutboxEventRepository repository = Mockito.mock(OutboxEventRepository.class);
        OutboxProperties properties = new OutboxProperties();
        properties.getPublisher().setMaxRetries(2);
        OutboxEvent event = event();
        event.setRetryCount(1);
        when(repository.findById(event.getId())).thenReturn(Optional.of(event));
        OutboxEventStateService service =
                new OutboxEventStateService(repository, properties, Clock.systemUTC());

        service.markFailed(event.getId(), new IllegalStateException("broker unavailable"));

        assertEquals(OutboxStatus.FAILED, event.getStatus());
        assertEquals(2, event.getRetryCount());
        assertNull(event.getNextRetryAt());
    }

    private OutboxEvent event() {
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setStatus(OutboxStatus.PROCESSING);
        return event;
    }
}
