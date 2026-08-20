package com.khanh.fooddelivery.tracking_service.dto.request;

import java.math.BigDecimal;
import java.time.Instant;

public record DriverLocationUpdateRequest(
        BigDecimal latitude,
        BigDecimal longitude,
        Double accuracyMeters,
        Instant recordedAt
) {
}
