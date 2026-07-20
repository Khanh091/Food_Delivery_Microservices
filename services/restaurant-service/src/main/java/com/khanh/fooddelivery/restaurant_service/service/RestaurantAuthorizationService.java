package com.khanh.fooddelivery.restaurant_service.service;

import com.khanh.fooddelivery.restaurant_service.enums.RestaurantMemberRole;
import java.util.UUID;

public interface RestaurantAuthorizationService {
    boolean isOwner(UUID restaurantId, UUID userId);

    boolean hasRestaurantRole(UUID restaurantId, UUID userId, RestaurantMemberRole... roles);

    boolean hasBranchAccess(UUID branchId, UUID userId, RestaurantMemberRole... roles);

    void requireRestaurantAccess(UUID restaurantId, UUID userId, RestaurantMemberRole... roles);

    void requireBranchAccess(UUID branchId, UUID userId, RestaurantMemberRole... roles);
}
