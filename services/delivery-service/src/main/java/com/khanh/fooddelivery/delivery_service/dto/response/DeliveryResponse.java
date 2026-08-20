package com.khanh.fooddelivery.delivery_service.dto.response;

import com.khanh.fooddelivery.delivery_service.model.DeliveryStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DeliveryResponse(
        UUID id,
        Long version,
        UUID orderId,
        UUID restaurantId,
        UUID branchId,
        UUID customerId,
        UUID driverId,
        DeliveryStatus status,
        String restaurantName,
        String branchName,
        String customerAddress,
        BigDecimal pickupLatitude,
        BigDecimal pickupLongitude,
        Instant createdAt,
        Instant updatedAt
) {
}
