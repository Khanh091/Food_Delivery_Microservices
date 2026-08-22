package com.khanh.fooddelivery.delivery_service.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderConfirmedEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        UUID aggregateId,
        int version,
        Payload payload
) {

    public static final String TYPE = "ORDER_CONFIRMED";
    public static final int VERSION = 1;

    public record Payload(
            UUID orderId,
            UUID restaurantId,
            UUID branchId,
            UUID customerId,
            String restaurantName,
            String branchName,
            String customerAddressLabel,
            String customerAddress,
            BigDecimal customerLatitude,
            BigDecimal customerLongitude
    ) {
    }
}
