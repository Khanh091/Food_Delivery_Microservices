package com.khanh.fooddelivery.catalog_service.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class OutboxEventClaimServiceTests {
    @Test
    void claimMarksOnlyConfiguredBatchAsProcessingWithLease() {
        OutboxEventRepository repository = Mockito.mock(OutboxEventRepository.class);
        OutboxProperties properties = new OutboxProperties();
        properties.getPublisher().setBatchSize(2);
        properties.getPublisher().setProcessingLeaseMs(30000);
        OutboxEvent event = new OutboxEvent();
        event.setStatus(OutboxStatus.PENDING);
        when(repository.lockNextPublishable(any(), eq(2))).thenReturn(List.of(event));
        Instant now = Instant.parse("2026-08-11T10:00:00Z");
        OutboxEventClaimService service =
                new OutboxEventClaimService(
                        repository, properties, Clock.fixed(now, ZoneOffset.UTC));

        List<OutboxEvent> claimed = service.claimNextBatch();

        assertEquals(1, claimed.size());
        assertEquals(OutboxStatus.PROCESSING, event.getStatus());
        assertEquals(now.plusSeconds(30), event.getNextRetryAt());
        verify(repository).lockNextPublishable(now, 2);
    }
}
