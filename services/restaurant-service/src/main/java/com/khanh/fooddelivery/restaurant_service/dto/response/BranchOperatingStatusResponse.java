package com.khanh.fooddelivery.restaurant_service.dto.response;

import java.time.LocalTime;
import java.time.OffsetDateTime;

public record BranchOperatingStatusResponse(
        boolean open,
        boolean withinBusinessHours,
        boolean acceptingOrders,
        boolean closedToday,
        LocalTime openTime,
        LocalTime closeTime,
        OffsetDateTime nextOpenAt,
        OffsetDateTime closeAt,
        String reason) {
    public BranchOperatingStatusResponse(
            boolean open,
            boolean withinBusinessHours,
            boolean acceptingOrders,
            boolean closedToday,
            LocalTime openTime,
            LocalTime closeTime,
            String reason) {
        this(
                open,
                withinBusinessHours,
                acceptingOrders,
                closedToday,
                openTime,
                closeTime,
                null,
                null,
                reason);
    }

    public BranchOperatingStatusResponse(boolean open, String reason) {
        this(open, open, true, false, null, null, null, null, reason);
    }
}
