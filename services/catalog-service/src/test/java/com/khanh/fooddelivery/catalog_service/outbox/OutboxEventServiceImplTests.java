package com.khanh.fooddelivery.catalog_service.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class OutboxEventServiceImplTests {
    @Test
    void enqueuePersistsVersionedEnvelopeAsPendingEvent() {
        OutboxEventRepository repository = Mockito.mock(OutboxEventRepository.class);
        OutboxAggregateVersionService aggregateVersionService =
                Mockito.mock(OutboxAggregateVersionService.class);
        Instant now = Instant.parse("2026-08-11T10:00:00Z");
        OutboxEventService service =
                new OutboxEventServiceImpl(
                        repository,
                        aggregateVersionService,
                        new ObjectMapper().findAndRegisterModules(),
                        Clock.fixed(now, ZoneOffset.UTC));
        UUID aggregateId = UUID.randomUUID();
        when(aggregateVersionService.nextVersion("CATALOG_ITEM", aggregateId)).thenReturn(7L);

        service.enqueue(
                CatalogEventType.CATALOG_ITEM_UPSERTED,
                "CATALOG_ITEM",
                aggregateId,
                Map.of("action", "CREATED", "itemId", aggregateId));

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());
        OutboxEvent event = captor.getValue();
        assertEquals(OutboxStatus.PENDING, event.getStatus());
        assertEquals(CatalogEventType.CATALOG_ITEM_UPSERTED.name(), event.getEventType());
        assertEquals(2, event.getEventVersion());
        assertEquals(aggregateId, event.getAggregateId());
        assertEquals(7L, event.getAggregateVersion());
        assertEquals(event.getId().toString(), event.getPayload().path("eventId").asText());
        assertEquals(7L, event.getPayload().path("aggregateVersion").asLong());
        assertEquals("CREATED", event.getPayload().path("data").path("action").asText());
        assertEquals(now, event.getCreatedAt());
    }
}
