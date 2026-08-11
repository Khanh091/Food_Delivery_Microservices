package com.khanh.fooddelivery.restaurant_service.service;

import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantBranchPublicAvailabilityResponse;
import java.util.UUID;

public interface RestaurantPublicAvailabilityService {
    RestaurantBranchPublicAvailabilityResponse getBranchPublicAvailability(
            UUID restaurantId, UUID branchId);
}
