package com.khanh.fooddelivery.search_service.projection;

import com.fasterxml.jackson.databind.JsonNode;
import com.khanh.fooddelivery.search_service.document.BranchSearchProjection;
import com.khanh.fooddelivery.search_service.document.CatalogItemSearchProjection;
import com.khanh.fooddelivery.search_service.event.DomainEventEnvelope;
import com.khanh.fooddelivery.search_service.repository.SearchProjectionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CatalogProjectionServiceImpl implements CatalogProjectionService {
    private final SearchProjectionRepository projectionRepository;

    public CatalogProjectionServiceImpl(SearchProjectionRepository projectionRepository) {
        this.projectionRepository = projectionRepository;
    }

    @Override
    public void apply(DomainEventEnvelope event) {
        switch (event.eventType()) {
            case "CATALOG_ITEM_UPSERTED", "CATALOG_ITEM_STATUS_CHANGED" ->
                    projectionRepository.applyCatalogItem(catalogItem(event));
            case "BRANCH_ITEM_UPSERTED",
                    "BRANCH_ITEM_PRICE_CHANGED",
                    "BRANCH_ITEM_AVAILABILITY_CHANGED" -> {
                UUID itemId = uuid(event.data(), "itemId");
                projectionRepository.applyBranchItem(itemId, branchItem(event));
            }
            default -> throw new IllegalArgumentException("Unsupported projection event: " + event.eventType());
        }
    }

    private CatalogItemSearchProjection catalogItem(DomainEventEnvelope event) {
        JsonNode data = event.data();
        return new CatalogItemSearchProjection(
                uuid(data, "itemId"),
                uuid(data, "restaurantId"),
                text(data, "name"),
                nullableText(data, "description"),
                text(data, "itemType"),
                decimal(data, "basePrice"),
                text(data, "currency"),
                nullableInteger(data, "preparationTimeMinutes"),
                data.path("isVegetarian").asBoolean(),
                text(data, "status"),
                nullableText(data, "primaryImageUrl"),
                event.aggregateVersion(),
                event.eventId());
    }

    private BranchSearchProjection branchItem(DomainEventEnvelope event) {
        JsonNode data = event.data();
        return new BranchSearchProjection(
                uuid(data, "branchItemId"),
                uuid(data, "branchId"),
                decimal(data, "sellingPrice"),
                nullableDecimal(data, "originalPrice"),
                data.path("isAvailable").asBoolean(),
                nullableInteger(data, "availableQuantity"),
                nullableInstant(data, "soldOutUntil"),
                event.aggregateVersion(),
                event.eventId());
    }

    private UUID uuid(JsonNode data, String field) {
        return UUID.fromString(text(data, field));
    }

    private String text(JsonNode data, String field) {
        JsonNode value = data.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            throw new IllegalArgumentException("Missing event field: " + field);
        }
        return value.asText();
    }

    private String nullableText(JsonNode data, String field) {
        JsonNode value = data.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private BigDecimal decimal(JsonNode data, String field) {
        JsonNode value = data.get(field);
        if (value == null || value.isNull()) {
            throw new IllegalArgumentException("Missing event field: " + field);
        }
        return value.decimalValue();
    }

    private BigDecimal nullableDecimal(JsonNode data, String field) {
        JsonNode value = data.get(field);
        return value == null || value.isNull() ? null : value.decimalValue();
    }

    private Integer nullableInteger(JsonNode data, String field) {
        JsonNode value = data.get(field);
        return value == null || value.isNull() ? null : value.intValue();
    }

    private Instant nullableInstant(JsonNode data, String field) {
        JsonNode value = data.get(field);
        return value == null || value.isNull() ? null : Instant.parse(value.asText());
    }
}
