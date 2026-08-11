package com.khanh.fooddelivery.restaurant_service.dto.response;

import java.util.UUID;

public record RestaurantBranchPublicAvailabilityResponse(
        UUID restaurantId,
        UUID branchId,
        boolean restaurantVisible,
        boolean branchVisible) {}
