package com.khanh.fooddelivery.restaurant_service.dto.request;

import com.khanh.fooddelivery.restaurant_service.enums.*;

public record RestaurantMemberUpdateRequest(
        RestaurantMemberRole role, RestaurantMemberStatus status) {}
