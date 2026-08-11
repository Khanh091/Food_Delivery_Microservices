package com.khanh.fooddelivery.catalog_service.dto.response;

import com.khanh.fooddelivery.catalog_service.enums.CatalogStatus;
import com.khanh.fooddelivery.catalog_service.enums.OptionSelectionType;
import java.time.Instant;
import java.util.UUID;

public record OptionGroupResponse(
        UUID id,
        UUID itemId,
        String name,
        OptionSelectionType selectionType,
        Integer minimumSelections,
        Integer maximumSelections,
        Boolean required,
        Integer sortOrder,
        CatalogStatus status,
        Instant createdAt,
        Instant updatedAt) {}
