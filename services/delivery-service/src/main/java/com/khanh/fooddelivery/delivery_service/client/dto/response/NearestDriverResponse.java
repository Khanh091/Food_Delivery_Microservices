package com.khanh.fooddelivery.delivery_service.client.dto.response;

import java.time.Instant;
import java.util.UUID;

public record NearestDriverResponse(
        UUID driverId,
        long distanceMeters,
        Instant locationUpdatedAt
) {
}
