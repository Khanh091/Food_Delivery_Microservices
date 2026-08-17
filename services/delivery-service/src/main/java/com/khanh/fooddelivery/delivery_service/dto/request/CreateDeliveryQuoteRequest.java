package com.khanh.fooddelivery.delivery_service.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateDeliveryQuoteRequest(@NotNull UUID branchId, @NotNull UUID addressId) {}
