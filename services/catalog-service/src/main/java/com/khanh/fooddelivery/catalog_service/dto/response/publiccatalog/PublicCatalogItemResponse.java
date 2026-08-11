package com.khanh.fooddelivery.catalog_service.dto.response.publiccatalog;

import com.khanh.fooddelivery.catalog_service.enums.CatalogItemType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PublicCatalogItemResponse(
        UUID id,
        String name,
        String description,
        CatalogItemType itemType,
        BigDecimal sellingPrice,
        BigDecimal originalPrice,
        String currency,
        boolean isAvailable,
        Integer availableQuantity,
        Instant soldOutUntil,
        Integer preparationTimeMinutes,
        Boolean isVegetarian,
        String primaryImageUrl,
        List<PublicItemImageResponse> images,
        List<PublicOptionGroupResponse> optionGroups) {}
