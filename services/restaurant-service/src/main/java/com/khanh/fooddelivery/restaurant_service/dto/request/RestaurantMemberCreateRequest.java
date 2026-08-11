package com.khanh.fooddelivery.restaurant_service.dto.request;

import com.khanh.fooddelivery.restaurant_service.enums.RestaurantMemberRole;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RestaurantMemberCreateRequest(
        @NotNull UUID userId, UUID branchId, @NotNull RestaurantMemberRole role) {}
