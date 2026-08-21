package com.khanh.fooddelivery.driver_service.dto.response;

import com.khanh.fooddelivery.driver_service.model.DriverStatus;
import com.khanh.fooddelivery.driver_service.model.VehicleType;
import java.time.Instant;
import java.util.UUID;

public record DriverProfileResponse(
        UUID id,
        Long version,
        UUID userId,
        DriverStatus status,
        VehicleType vehicleType,
        String vehiclePlate,
        Instant createdAt,
        Instant updatedAt
) {
}
