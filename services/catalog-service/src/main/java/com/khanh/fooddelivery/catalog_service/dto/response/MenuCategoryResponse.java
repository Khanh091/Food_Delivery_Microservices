package com.khanh.fooddelivery.catalog_service.dto.response;

import com.khanh.fooddelivery.catalog_service.enums.CatalogStatus;
import java.time.Instant;
import java.util.UUID;

public record MenuCategoryResponse(
        UUID id,
        UUID menuId,
        String name,
        String description,
        Integer sortOrder,
        CatalogStatus status,
        Instant createdAt,
        Instant updatedAt) {}
