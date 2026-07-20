package com.khanh.fooddelivery.restaurant_service.dto.response;

import java.time.LocalTime;
import java.util.UUID;

public record BranchBusinessHourResponse(
        UUID id,
        UUID branchId,
        Short dayOfWeek,
        LocalTime openTime,
        LocalTime closeTime,
        boolean closed,
        long version) {}
