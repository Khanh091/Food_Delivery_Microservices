package com.khanh.fooddelivery.search_service.consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khanh.fooddelivery.search_service.projection.CatalogProjectionService;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CatalogEventConsumerTests {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void supportedEventInvokesProjection() throws Exception {
        CatalogProjectionService projectionService = Mockito.mock(CatalogProjectionService.class);
        CatalogEventConsumer consumer = new CatalogEventConsumer(objectMapper, projectionService);

        consumer.consume(event("CATALOG_ITEM_UPSERTED", 2));

        verify(projectionService).apply(any());
    }

    @Test
    void knownOutOfScopeEventIsIntentionallyIgnored() throws Exception {
        CatalogProjectionService projectionService = Mockito.mock(CatalogProjectionService.class);
        CatalogEventConsumer consumer = new CatalogEventConsumer(objectMapper, projectionService);

        consumer.consume(event("MENU_CHANGED", 2));

        verifyNoInteractions(projectionService);
    }

    @Test
    void unsupportedEventVersionIsNotAcknowledgedAsSuccess() throws Exception {
        CatalogProjectionService projectionService = Mockito.mock(CatalogProjectionService.class);
        CatalogEventConsumer consumer = new CatalogEventConsumer(objectMapper, projectionService);

        assertThatThrownBy(() -> consumer.consume(event("CATALOG_ITEM_UPSERTED", 1)))
                .isInstanceOf(UnsupportedCatalogEventVersionException.class);
        verifyNoInteractions(projectionService);
    }

    @Test
    void invalidEnvelopeIsNotSilentlyIndexed() {
        CatalogProjectionService projectionService = Mockito.mock(CatalogProjectionService.class);
        CatalogEventConsumer consumer = new CatalogEventConsumer(objectMapper, projectionService);

        assertThatThrownBy(() -> consumer.consume("not-json"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(projectionService);
    }

    @Test
    void projectionFailurePropagatesForKafkaRetry() throws Exception {
        CatalogProjectionService projectionService = Mockito.mock(CatalogProjectionService.class);
        doThrow(new IllegalStateException("Elasticsearch unavailable"))
                .when(projectionService)
                .apply(any());
        CatalogEventConsumer consumer = new CatalogEventConsumer(objectMapper, projectionService);

        assertThatThrownBy(() -> consumer.consume(event("CATALOG_ITEM_UPSERTED", 2)))
                .isInstanceOf(IllegalStateException.class);
        verify(projectionService).apply(any());
    }

    private String event(String eventType, int eventVersion) throws Exception {
        return objectMapper.writeValueAsString(
                Map.of(
                        "eventId", UUID.randomUUID(),
                        "eventType", eventType,
                        "eventVersion", eventVersion,
                        "aggregateType", "CATALOG_ITEM",
                        "aggregateId", UUID.randomUUID(),
                        "aggregateVersion", 1,
                        "occurredAt", Instant.now(),
                        "data",
                        Map.of(
                                "itemId", UUID.randomUUID(),
                                "restaurantId", UUID.randomUUID(),
                                "name", "Pho Bo",
                                "itemType", "FOOD",
                                "basePrice", 50000,
                                "currency", "VND",
                                "isVegetarian", false,
                                "status", "ACTIVE")));
    }
}
