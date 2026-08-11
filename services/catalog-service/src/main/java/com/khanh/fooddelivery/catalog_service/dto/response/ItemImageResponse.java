package com.khanh.fooddelivery.catalog_service.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ItemImageResponse(
        UUID id,
        UUID itemId,
        String imageUrl,
        Integer sortOrder,
        Boolean isPrimary,
        Instant createdAt,
        Instant updatedAt) {}
