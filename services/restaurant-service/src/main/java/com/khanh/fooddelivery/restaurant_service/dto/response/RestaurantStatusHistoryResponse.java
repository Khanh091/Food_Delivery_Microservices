package com.khanh.fooddelivery.restaurant_service.dto.response;

import com.khanh.fooddelivery.restaurant_service.enums.RestaurantStatus;
import java.time.Instant;
import java.util.UUID;

public record RestaurantStatusHistoryResponse(
        UUID id,
        UUID restaurantId,
        RestaurantStatus oldStatus,
        RestaurantStatus newStatus,
        String reason,
        UUID changedByUserId,
        Instant changedAt,
        String createdBy) {}
