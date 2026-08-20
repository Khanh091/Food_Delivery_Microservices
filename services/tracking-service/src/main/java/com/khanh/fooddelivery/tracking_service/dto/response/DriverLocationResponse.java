package com.khanh.fooddelivery.tracking_service.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DriverLocationResponse(
        UUID driverId,
        BigDecimal latitude,
        BigDecimal longitude,
        Double accuracyMeters,
        Instant recordedAt,
        Instant updatedAt
) {
}
