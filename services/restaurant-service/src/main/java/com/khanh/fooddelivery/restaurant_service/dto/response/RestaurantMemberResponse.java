package com.khanh.fooddelivery.restaurant_service.dto.response;

import com.khanh.fooddelivery.restaurant_service.enums.RestaurantMemberRole;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantMemberStatus;
import java.time.Instant;
import java.util.UUID;

public record RestaurantMemberResponse(
        UUID id,
        UUID restaurantId,
        UUID branchId,
        UUID userId,
        RestaurantMemberRole role,
        RestaurantMemberStatus status,
        UUID invitedByUserId,
        Instant joinedAt,
        Instant createdAt,
        long version) {}
