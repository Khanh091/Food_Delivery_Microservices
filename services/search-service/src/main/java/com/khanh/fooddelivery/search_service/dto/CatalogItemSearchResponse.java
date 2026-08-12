package com.khanh.fooddelivery.search_service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CatalogItemSearchResponse(
        UUID itemId,
        UUID restaurantId,
        UUID branchId,
        String name,
        String description,
        String itemType,
        BigDecimal sellingPrice,
        BigDecimal originalPrice,
        boolean isAvailable,
        Integer availableQuantity,
        Instant soldOutUntil,
        boolean vegetarian,
        Integer preparationTimeMinutes) {}
