package com.khanh.fooddelivery.restaurant_service.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record BranchSpecialHourResponse(
        UUID id,
        UUID branchId,
        LocalDate specialDate,
        LocalTime openTime,
        LocalTime closeTime,
        boolean closed,
        String reason,
        long version) {}
