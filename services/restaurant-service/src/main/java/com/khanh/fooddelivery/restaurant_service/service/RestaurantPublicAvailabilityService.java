package com.khanh.fooddelivery.restaurant_service.service;

import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantBranchPublicAvailabilityResponse;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantBranchCartAvailabilityResponse;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantBranchOrderingContextResponse;
import java.util.UUID;

public interface RestaurantPublicAvailabilityService {
    RestaurantBranchPublicAvailabilityResponse getBranchPublicAvailability(
            UUID restaurantId, UUID branchId);

    RestaurantBranchCartAvailabilityResponse getBranchCartAvailability(UUID restaurantId, UUID branchId);

    RestaurantBranchOrderingContextResponse getBranchOrderingContext(UUID branchId);
}
