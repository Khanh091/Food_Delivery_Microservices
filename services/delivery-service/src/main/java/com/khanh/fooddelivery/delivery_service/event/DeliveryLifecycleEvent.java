package com.khanh.fooddelivery.delivery_service.event;

import java.time.Instant;
import java.util.UUID;

public record DeliveryLifecycleEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        UUID aggregateId,
        int version,
        Payload payload
) {

    public static final String DELIVERY_OFFER_CREATED = "DELIVERY_OFFER_CREATED";
    public static final int VERSION = 1;

    public record Payload(
            UUID offerId,
            UUID deliveryId,
            UUID driverId,
            Instant expiresAt
    ) {
    }
}
