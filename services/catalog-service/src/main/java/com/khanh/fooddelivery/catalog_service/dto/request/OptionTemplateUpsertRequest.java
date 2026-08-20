package com.khanh.fooddelivery.catalog_service.dto.request;

import com.khanh.fooddelivery.catalog_service.enums.OptionSelectionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record OptionTemplateUpsertRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull OptionSelectionType selectionType,
        @NotNull @Min(0) Integer minimumSelections,
        @NotNull @Min(1) Integer maximumSelections,
        @Min(0) Integer sortOrder,
        @NotEmpty List<@Valid OptionTemplateValueInput> values) {}
