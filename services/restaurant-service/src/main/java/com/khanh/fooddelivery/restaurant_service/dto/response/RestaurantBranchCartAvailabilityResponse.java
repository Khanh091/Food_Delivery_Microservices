package com.khanh.fooddelivery.restaurant_service.dto.response;

import java.util.UUID;

public record RestaurantBranchCartAvailabilityResponse(
        UUID restaurantId,
        String restaurantName,
        boolean restaurantActive,
        UUID branchId,
        String branchName,
        boolean branchActive,
        boolean acceptingOrders) {}
