package com.khanh.fooddelivery.catalog_service.dto.response;

import java.time.Instant;
import java.util.UUID;

public record MenuCategoryItemResponse(
        UUID id,
        UUID categoryId,
        UUID itemId,
        Integer sortOrder,
        Instant createdAt,
        Instant updatedAt) {}
