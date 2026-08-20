package com.khanh.fooddelivery.catalog_service.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record MenuCategoryItemBatchCreateRequest(@NotEmpty List<@NotNull UUID> itemIds) {}
