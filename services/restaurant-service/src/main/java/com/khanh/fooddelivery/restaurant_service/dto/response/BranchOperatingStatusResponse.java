package com.khanh.fooddelivery.restaurant_service.dto.response;

import java.time.LocalTime;

public record BranchOperatingStatusResponse(
        boolean open,
        boolean withinBusinessHours,
        boolean acceptingOrders,
        boolean closedToday,
        LocalTime openTime,
        LocalTime closeTime,
        String reason) {
    public BranchOperatingStatusResponse(boolean open, String reason) {
        this(open, open, true, false, null, null, reason);
    }
}
