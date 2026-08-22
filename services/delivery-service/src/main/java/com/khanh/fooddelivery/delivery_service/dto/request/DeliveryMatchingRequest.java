package com.khanh.fooddelivery.delivery_service.dto.request;

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
        BigDecimal customerLongitude,
        String paymentMethod,
        BigDecimal requiredRestaurantAdvance,
        BigDecimal customerCashToCollect,
        BigDecimal driverGrossEarning,
        BigDecimal restaurantCommissionAmount,
        BigDecimal driverCommissionAmount,
        BigDecimal driverNetEarning,
        BigDecimal restaurantNetAmount,
        BigDecimal platformRevenueAmount
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
                customerAddress, customerLatitude, customerLongitude, null, null, null, null, null, null, null, null, null);
    }

    public DeliveryMatchingRequest(
            UUID orderId, UUID restaurantId, UUID branchId, UUID customerId,
            String restaurantName, String branchName, String customerAddressLabel,
            String customerAddress, BigDecimal customerLatitude, BigDecimal customerLongitude
    ) {
        this(orderId, restaurantId, branchId, customerId, restaurantName, branchName, customerAddressLabel,
                customerAddress, customerLatitude, customerLongitude, null, null, null, null, null, null, null, null, null);
    }
}
