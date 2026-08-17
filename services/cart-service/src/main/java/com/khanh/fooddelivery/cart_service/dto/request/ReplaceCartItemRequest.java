package com.khanh.fooddelivery.cart_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReplaceCartItemRequest(
        @NotNull @Min(1) Long expectedCartVersion, @NotNull AddCartItemRequest item) {}
