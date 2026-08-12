package com.khanh.fooddelivery.search_service.document;

import java.util.UUID;
public record RestaurantSearchProjection(UUID restaurantId, String name, String description, String status,
        String restaurantCode, String logoUrl, String coverImageUrl, long aggregateVersion, UUID lastEventId) {}
