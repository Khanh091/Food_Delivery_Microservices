package com.khanh.fooddelivery.catalog_service.dto.request.internal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record CheckoutItemsValidationRequest(
        @NotNull UUID restaurantId,
        @NotNull UUID branchId,
        @NotEmpty List<@Valid CheckoutItemRequest> items) {
    public record CheckoutItemRequest(
            @NotNull UUID cartItemId,
            @NotNull UUID catalogItemId,
            @NotNull List<UUID> selectedOptionValueIds) {}
}
