package com.khanh.fooddelivery.catalog_service.dto.request;

import com.khanh.fooddelivery.catalog_service.enums.OptionSelectionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record OptionGroupUpdateRequest(
        @Size(max = 255) String name,
        OptionSelectionType selectionType,
        @Min(0) Integer minimumSelections,
        @Min(1) Integer maximumSelections,
        Boolean required,
        @Min(0) Integer sortOrder) {}
