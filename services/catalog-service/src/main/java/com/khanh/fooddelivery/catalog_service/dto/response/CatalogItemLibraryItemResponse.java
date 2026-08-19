package com.khanh.fooddelivery.catalog_service.dto.response;

import com.khanh.fooddelivery.catalog_service.enums.CatalogItemType;
import com.khanh.fooddelivery.catalog_service.enums.CatalogStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record CatalogItemLibraryItemResponse(
        UUID id,
        UUID restaurantId,
        String name,
        String description,
        CatalogItemType itemType,
        BigDecimal basePrice,
        String currency,
        Integer preparationTimeMinutes,
        Boolean isVegetarian,
        CatalogStatus status,
        String primaryImageUrl,
        long placementCount) {}
