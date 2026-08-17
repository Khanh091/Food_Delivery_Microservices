package com.khanh.fooddelivery.cart_service.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateCartItemQuantityRequest(@Min(1) @Max(99) int quantity) {}
