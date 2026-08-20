package com.khanh.fooddelivery.driver_service.dto.response;

import java.time.Instant;
import java.util.UUID;

public record DriverAvailabilityResponse(
        UUID id,
        Long version,
        UUID userId,
        boolean available,
        UUID activeDeliveryId,
        UUID pendingOfferDeliveryId,
        Instant updatedAt
) {
}
