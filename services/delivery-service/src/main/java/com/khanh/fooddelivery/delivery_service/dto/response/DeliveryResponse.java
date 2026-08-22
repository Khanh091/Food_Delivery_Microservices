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
        String pickupAddress,
        String customerAddressLabel,
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
        BigDecimal platformRevenueAmount,
        boolean restaurantAdvanceConfirmed,
        boolean customerCashCollected,
        Instant createdAt,
        Instant updatedAt
) {
    public DeliveryResponse(
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
        this(id, version, orderId, restaurantId, branchId, customerId, driverId, status, restaurantName,
                branchName, customerAddress, pickupLatitude, pickupLongitude, null, null, null, null,
                null, null, null, null, null, null, null, null, null, false, false,
                createdAt, updatedAt);
    }
}
