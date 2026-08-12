package com.khanh.fooddelivery.search_service.document;

import java.math.BigDecimal;
import java.util.UUID;

public record CatalogItemSearchProjection(
        UUID itemId,
        UUID restaurantId,
        String name,
        String description,
        String itemType,
        BigDecimal basePrice,
        String currency,
        Integer preparationTimeMinutes,
        boolean vegetarian,
        String status,
        long aggregateVersion,
        UUID lastEventId) {}
