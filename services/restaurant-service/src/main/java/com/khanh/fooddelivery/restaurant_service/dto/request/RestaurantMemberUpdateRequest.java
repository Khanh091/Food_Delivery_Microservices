package com.khanh.fooddelivery.restaurant_service.dto.request;

import com.khanh.fooddelivery.restaurant_service.enums.RestaurantMemberRole;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantMemberStatus;

public record RestaurantMemberUpdateRequest(
        RestaurantMemberRole role, RestaurantMemberStatus status) {}
