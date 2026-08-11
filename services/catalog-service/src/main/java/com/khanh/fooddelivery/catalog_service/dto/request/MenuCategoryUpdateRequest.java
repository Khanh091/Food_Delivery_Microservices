package com.khanh.fooddelivery.catalog_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record MenuCategoryUpdateRequest(
        @Size(max = 255) String name,
        @Size(max = 5000) String description,
        @Min(0) Integer sortOrder) {}
