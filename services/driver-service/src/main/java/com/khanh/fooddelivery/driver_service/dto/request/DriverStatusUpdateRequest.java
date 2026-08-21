package com.khanh.fooddelivery.driver_service.dto.request;

import com.khanh.fooddelivery.driver_service.model.DriverStatus;
import jakarta.validation.constraints.NotNull;

public record DriverStatusUpdateRequest(@NotNull DriverStatus status) {
}
