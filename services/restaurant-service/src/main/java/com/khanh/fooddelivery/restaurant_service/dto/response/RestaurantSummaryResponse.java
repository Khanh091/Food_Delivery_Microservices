package com.khanh.fooddelivery.restaurant_service.dto.response;

import com.khanh.fooddelivery.restaurant_service.enums.RestaurantStatus;
import java.util.UUID;

public record RestaurantSummaryResponse(
        UUID id, String restaurantCode, String name, RestaurantStatus status) {}
