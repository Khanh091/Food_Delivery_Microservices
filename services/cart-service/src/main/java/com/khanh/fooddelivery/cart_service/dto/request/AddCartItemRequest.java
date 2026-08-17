package com.khanh.fooddelivery.cart_service.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record AddCartItemRequest(
        @NotNull UUID restaurantId,
        @NotNull UUID branchId,
        @NotNull UUID catalogItemId,
        @Min(1) @Max(99) int quantity,
        @NotNull List<UUID> selectedOptionValueIds,
        String note) {}
