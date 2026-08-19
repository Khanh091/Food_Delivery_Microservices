package com.khanh.fooddelivery.restaurant_service.dto.request;

import com.khanh.fooddelivery.restaurant_service.enums.RestaurantMemberRole;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantMemberStatus;
import java.util.UUID;

public record RestaurantMemberUpdateRequest(
        RestaurantMemberRole role,
        RestaurantMemberStatus status,
        UUID branchId,
        Boolean updateBranchScope) {}
