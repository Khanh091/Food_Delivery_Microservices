package com.khanh.fooddelivery.catalog_service.dto.response;

import com.khanh.fooddelivery.catalog_service.enums.CatalogStatus;
import com.khanh.fooddelivery.catalog_service.enums.OptionSelectionType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OptionTemplateResponse(
        UUID id,
        UUID restaurantId,
        String name,
        OptionSelectionType selectionType,
        Integer minimumSelections,
        Integer maximumSelections,
        Boolean required,
        Integer sortOrder,
        CatalogStatus status,
        List<OptionTemplateValueResponse> values,
        Instant createdAt,
        Instant updatedAt) {}
