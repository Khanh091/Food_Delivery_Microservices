package com.khanh.fooddelivery.catalog_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MenuCategoryItemSortOrderUpdateRequest(@NotNull @Min(0) Integer sortOrder) {}
