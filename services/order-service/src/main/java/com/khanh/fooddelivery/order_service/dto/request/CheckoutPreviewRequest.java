package com.khanh.fooddelivery.order_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CheckoutPreviewRequest(@Min(1) long cartVersion, @NotNull UUID addressId) {}
