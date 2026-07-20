package com.khanh.fooddelivery.restaurant_service.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record BranchSpecialHourCreateRequest(
        @NotNull LocalDate specialDate,
        LocalTime openTime,
        LocalTime closeTime,
        @NotNull Boolean isClosed,
        @Size(max = 255) String reason) {}
