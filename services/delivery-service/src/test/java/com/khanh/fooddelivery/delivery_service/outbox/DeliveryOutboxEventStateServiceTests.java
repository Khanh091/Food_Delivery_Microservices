package com.khanh.fooddelivery.delivery_service.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeliveryOutboxEventStateServiceTests {

    @Mock
    private DeliveryOutboxEventRepository repository;

    private DeliveryOutboxEventStateService states;
    private DeliveryOutboxProperties properties;

    @BeforeEach
    void setUp() {
        properties = new DeliveryOutboxProperties();
        properties.getPublisher().setRetryDelayMs(5000);
        states = new DeliveryOutboxEventStateService(
                repository,
                properties,
                Clock.fixed(Instant.parse("2026-08-23T10:15:30Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void markPublishedSetsTimestampAndClearsClaim() {
        UUID id = UUID.randomUUID();
        UUID claimToken = UUID.randomUUID();
        DeliveryOutboxEvent event = event(id, claimToken);
        when(repository.findById(id)).thenReturn(Optional.of(event));

        states.markPublished(id, claimToken);

        assertThat(event.getPublishedAt()).isEqualTo(Instant.parse("2026-08-23T10:15:30Z"));
        assertThat(event.getClaimedAt()).isNull();
        assertThat(event.getClaimToken()).isNull();
        assertThat(event.getNextAttemptAt()).isNull();
    }

    @Test
    void markFailedIncrementsRetryMetadataWithoutPublishing() {
        UUID id = UUID.randomUUID();
        UUID claimToken = UUID.randomUUID();
        DeliveryOutboxEvent event = event(id, claimToken);
        when(repository.findById(id)).thenReturn(Optional.of(event));

        states.markFailed(id, claimToken, new IllegalStateException("broker unavailable"));

        assertThat(event.getPublishedAt()).isNull();
        assertThat(event.getAttemptCount()).isEqualTo(1);
        assertThat(event.getLastError()).contains("broker unavailable");
        assertThat(event.getNextAttemptAt()).isEqualTo(Instant.parse("2026-08-23T10:15:35Z"));
        assertThat(event.getClaimedAt()).isNull();
        assertThat(event.getClaimToken()).isNull();
    }

    private DeliveryOutboxEvent event(UUID id, UUID claimToken) {
        DeliveryOutboxEvent event = new DeliveryOutboxEvent();
        event.setId(id);
        event.setClaimToken(claimToken);
        event.setAttemptCount(0);
        return event;
    }
}
