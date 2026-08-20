package com.khanh.fooddelivery.order_service.client.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

public record DeliveryMatchingRequest(
        UUID orderId,
        UUID restaurantId,
        UUID branchId,
        UUID customerId,
        String restaurantName,
        String branchName,
        String customerAddress,
        BigDecimal customerLatitude,
        BigDecimal customerLongitude
) {
}
