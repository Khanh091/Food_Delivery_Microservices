package com.khanh.fooddelivery.restaurant_service.dto.request;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record BranchSpecialHourUpdateRequest(
        LocalDate specialDate,
        LocalTime openTime,
        LocalTime closeTime,
        Boolean isClosed,
        @Size(max = 255) String reason) {}
