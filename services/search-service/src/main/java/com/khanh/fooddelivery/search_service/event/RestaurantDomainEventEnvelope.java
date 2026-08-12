package com.khanh.fooddelivery.search_service.event;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record RestaurantDomainEventEnvelope(
        UUID eventId, String eventType, int eventVersion, String aggregateType, UUID aggregateId,
        long aggregateVersion, Instant occurredAt, JsonNode data) {}
