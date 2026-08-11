package com.khanh.fooddelivery.catalog_service.dto.request;

import jakarta.validation.constraints.Min;

public record MenuCategoryItemCreateRequest(@Min(0) Integer sortOrder) {}
