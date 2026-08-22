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
        String customerAddressLabel,
        String customerAddress,
        BigDecimal customerLatitude,
        BigDecimal customerLongitude
) {
    public DeliveryMatchingRequest(
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
        this(orderId, restaurantId, branchId, customerId, restaurantName, branchName, null,
                customerAddress, customerLatitude, customerLongitude);
    }
}
