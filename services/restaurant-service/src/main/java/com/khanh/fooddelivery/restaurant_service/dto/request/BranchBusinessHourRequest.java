package com.khanh.fooddelivery.restaurant_service.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record BranchBusinessHourRequest(
        @NotNull @Min(1) @Max(7) Short dayOfWeek,
        LocalTime openTime,
        LocalTime closeTime,
        @NotNull Boolean isClosed) {}
