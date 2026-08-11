package com.khanh.fooddelivery.catalog_service.dto.request;

import com.khanh.fooddelivery.catalog_service.enums.OptionSelectionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OptionGroupCreateRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull OptionSelectionType selectionType,
        @NotNull @Min(0) Integer minimumSelections,
        @NotNull @Min(1) Integer maximumSelections,
        @NotNull Boolean required,
        @Min(0) Integer sortOrder) {}
