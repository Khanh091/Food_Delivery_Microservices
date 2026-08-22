package com.khanh.fooddelivery.delivery_service.event;

import java.time.Instant;
import java.util.UUID;

public record DeliveryOfferCreatedEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        UUID aggregateId,
        int version,
        Payload payload
) {

    public static final String EVENT_TYPE = "DELIVERY_OFFER_CREATED";
    public static final int VERSION = 1;

    public record Payload(
            UUID offerId,
            UUID deliveryId,
            UUID driverId,
            Instant expiresAt
    ) {
    }
}
