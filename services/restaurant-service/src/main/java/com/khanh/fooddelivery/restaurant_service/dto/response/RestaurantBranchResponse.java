package com.khanh.fooddelivery.restaurant_service.dto.response;

import com.khanh.fooddelivery.restaurant_service.enums.RestaurantBranchStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RestaurantBranchResponse(
        UUID id,
        UUID restaurantId,
        String branchCode,
        String name,
        String phoneNumber,
        String email,
        String addressLine,
        String ward,
        String district,
        String city,
        BigDecimal latitude,
        BigDecimal longitude,
        RestaurantBranchStatus status,
        boolean acceptingOrders,
        BigDecimal minimumOrderAmount,
        Integer defaultPreparationMinutes,
        Instant createdAt,
        Instant updatedAt,
        long version) {}
