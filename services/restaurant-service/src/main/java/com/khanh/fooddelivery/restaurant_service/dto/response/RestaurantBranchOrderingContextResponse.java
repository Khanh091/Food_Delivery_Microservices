package com.khanh.fooddelivery.restaurant_service.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record RestaurantBranchOrderingContextResponse(
        UUID restaurantId,
        String restaurantName,
        boolean restaurantActive,
        UUID branchId,
        String branchName,
        boolean branchActive,
        boolean acceptingOrders,
        String addressLine,
        String ward,
        String district,
        String city,
        BigDecimal latitude,
        BigDecimal longitude) {
    public RestaurantBranchOrderingContextResponse(
            UUID restaurantId,
            String restaurantName,
            boolean restaurantActive,
            UUID branchId,
            String branchName,
            boolean branchActive,
            boolean acceptingOrders,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        this(restaurantId, restaurantName, restaurantActive, branchId, branchName, branchActive,
                acceptingOrders, null, null, null, null, latitude, longitude);
    }
}
