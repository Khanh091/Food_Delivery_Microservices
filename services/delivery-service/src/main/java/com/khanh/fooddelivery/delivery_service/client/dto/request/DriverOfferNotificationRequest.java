package com.khanh.fooddelivery.delivery_service.client.dto.request;

import java.util.UUID;

/** Minimal, non-authoritative signal sent to notification-service. */
public record DriverOfferNotificationRequest(
        UUID driverId,
        UUID offerId,
        UUID deliveryId
) {
}
