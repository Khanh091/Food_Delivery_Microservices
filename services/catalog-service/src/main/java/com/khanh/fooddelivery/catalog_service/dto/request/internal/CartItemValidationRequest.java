package com.khanh.fooddelivery.catalog_service.dto.request.internal;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record CartItemValidationRequest(
        @NotNull UUID restaurantId,
        @NotNull UUID branchId,
        @NotNull UUID catalogItemId,
        @NotNull List<UUID> selectedOptionValueIds) {}
