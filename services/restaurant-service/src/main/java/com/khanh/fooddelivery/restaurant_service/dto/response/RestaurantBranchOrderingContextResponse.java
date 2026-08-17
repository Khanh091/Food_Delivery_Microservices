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
        BigDecimal latitude,
        BigDecimal longitude) {}
