package com.khanh.fooddelivery.restaurant_service.dto.request;

import com.khanh.fooddelivery.restaurant_service.enums.RestaurantMemberRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RestaurantMemberCreateRequest(
        @NotBlank @Email String email, UUID branchId, @NotNull RestaurantMemberRole role) {}
