package com.khanh.fooddelivery.tracking_service.dto.response;

import java.time.Instant;
import java.util.UUID;

public record NearestDriverResponse(UUID driverId, long distanceMeters, Instant locationUpdatedAt) {
}
