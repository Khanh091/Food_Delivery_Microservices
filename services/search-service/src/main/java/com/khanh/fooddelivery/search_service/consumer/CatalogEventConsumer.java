package com.khanh.fooddelivery.search_service.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.khanh.fooddelivery.search_service.event.CatalogEventType;
import com.khanh.fooddelivery.search_service.event.DomainEventEnvelope;
import com.khanh.fooddelivery.search_service.projection.CatalogProjectionService;
import java.util.EnumSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CatalogEventConsumer {
    private static final int SUPPORTED_EVENT_VERSION = 2;
    private static final EnumSet<CatalogEventType> PROJECTION_EVENTS =
            EnumSet.of(
                    CatalogEventType.CATALOG_ITEM_UPSERTED,
                    CatalogEventType.CATALOG_ITEM_STATUS_CHANGED,
                    CatalogEventType.BRANCH_ITEM_UPSERTED,
                    CatalogEventType.BRANCH_ITEM_PRICE_CHANGED,
                    CatalogEventType.BRANCH_ITEM_AVAILABILITY_CHANGED);

    private final ObjectMapper objectMapper;
    private final CatalogProjectionService projectionService;

    public CatalogEventConsumer(
            ObjectMapper objectMapper, CatalogProjectionService projectionService) {
        this.objectMapper = objectMapper;
        this.projectionService = projectionService;
    }

    @KafkaListener(topics = "${app.search.catalog-events-topic}")
    public void consume(String payload) {
        DomainEventEnvelope event = deserialize(payload);
        validateEnvelope(event);
        if (event.eventVersion() != SUPPORTED_EVENT_VERSION) {
            throw new UnsupportedCatalogEventVersionException(event);
        }

        CatalogEventType eventType = parseEventType(event);
        if (!PROJECTION_EVENTS.contains(eventType)) {
            log.debug(
                    "Ignoring catalog event outside Search Part 1 eventId={} eventType={}",
                    event.eventId(),
                    event.eventType());
            return;
        }

        projectionService.apply(event);
    }

    private DomainEventEnvelope deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, DomainEventEnvelope.class);
        } catch (JsonProcessingException exception) {
            throw new InvalidCatalogEventEnvelopeException("Invalid catalog event envelope", exception);
        }
    }

    private void validateEnvelope(DomainEventEnvelope event) {
        if (event.eventId() == null
                || isBlank(event.eventType())
                || event.eventVersion() < 1
                || isBlank(event.aggregateType())
                || event.aggregateId() == null
                || event.aggregateVersion() < 1
                || event.occurredAt() == null
                || event.data() == null
                || !event.data().isObject()) {
            throw new InvalidCatalogEventEnvelopeException("Catalog event envelope has missing required fields");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private CatalogEventType parseEventType(DomainEventEnvelope event) {
        try {
            return CatalogEventType.valueOf(event.eventType());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown catalog event type: " + event.eventType(), exception);
        }
    }
}
