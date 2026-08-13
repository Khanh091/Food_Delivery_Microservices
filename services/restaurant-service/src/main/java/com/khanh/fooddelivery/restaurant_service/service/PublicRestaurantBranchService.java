package com.khanh.fooddelivery.restaurant_service.service;

import com.khanh.fooddelivery.restaurant_service.dto.response.PublicRestaurantBranchResponse;
import java.util.UUID;

public interface PublicRestaurantBranchService {
    PublicRestaurantBranchResponse get(UUID restaurantId, UUID branchId);
}
