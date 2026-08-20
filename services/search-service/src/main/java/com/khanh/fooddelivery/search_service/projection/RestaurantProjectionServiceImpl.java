package com.khanh.fooddelivery.search_service.projection;

import com.fasterxml.jackson.databind.JsonNode;
import com.khanh.fooddelivery.search_service.document.RestaurantBranchSearchProjection;
import com.khanh.fooddelivery.search_service.document.RestaurantSearchProjection;
import com.khanh.fooddelivery.search_service.event.RestaurantDomainEventEnvelope;
import com.khanh.fooddelivery.search_service.repository.RestaurantSearchProjectionRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RestaurantProjectionServiceImpl
        implements RestaurantProjectionService {

    private final RestaurantSearchProjectionRepository repository;

    public RestaurantProjectionServiceImpl(
            RestaurantSearchProjectionRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public void apply(RestaurantDomainEventEnvelope event) {
        switch (event.eventType()) {
            case "RESTAURANT_UPSERTED",
                 "RESTAURANT_STATUS_CHANGED" ->
                    repository.applyRestaurant(
                            restaurant(event)
                    );

            case "RESTAURANT_BRANCH_UPSERTED",
                 "RESTAURANT_BRANCH_STATUS_CHANGED" ->
                    repository.applyBranch(
                            branch(event),
                            uuid(event.data(), "restaurantId")
                    );

            default ->
                    throw new IllegalArgumentException(
                            "Unsupported restaurant projection event: "
                                    + event.eventType()
                    );
        }
    }

    private RestaurantSearchProjection restaurant(
            RestaurantDomainEventEnvelope event
    ) {
        JsonNode data = event.data();

        return new RestaurantSearchProjection(
                uuid(data, "restaurantId"),
                text(data, "name"),
                nullable(data, "description"),
                text(data, "status"),
                text(data, "restaurantCode"),
                nullable(data, "logoUrl"),
                nullable(data, "coverImageUrl"),
                event.aggregateVersion(),
                event.eventId()
        );
    }

    private RestaurantBranchSearchProjection branch(
            RestaurantDomainEventEnvelope event
    ) {
        JsonNode data = event.data();

        return new RestaurantBranchSearchProjection(
                uuid(data, "branchId"),
                text(data, "name"),
                text(data, "status"),
                text(data, "addressLine"),
                nullable(data, "ward"),
                nullable(data, "district"),
                nullable(data, "city"),
                decimal(data, "latitude"),
                decimal(data, "longitude"),
                data.path("acceptingOrders").asBoolean(),
                event.aggregateVersion(),
                event.eventId()
        );
    }

    private UUID uuid(
            JsonNode data,
            String field
    ) {
        return UUID.fromString(
                text(data, field)
        );
    }

    private String text(
            JsonNode data,
            String field
    ) {
        JsonNode node = data.get(field);

        if (node == null
                || node.isNull()
                || node.asText().isBlank()) {
            throw new IllegalArgumentException(
                    "Missing event field: " + field
            );
        }

        return node.asText();
    }

    private String nullable(
            JsonNode data,
            String field
    ) {
        JsonNode node = data.get(field);

        return node == null || node.isNull()
                ? null
                : node.asText();
    }

    private BigDecimal decimal(
            JsonNode data,
            String field
    ) {
        JsonNode node = data.get(field);

        if (node == null || node.isNull()) {
            throw new IllegalArgumentException(
                    "Missing event field: " + field
            );
        }

        return node.decimalValue();
    }
}