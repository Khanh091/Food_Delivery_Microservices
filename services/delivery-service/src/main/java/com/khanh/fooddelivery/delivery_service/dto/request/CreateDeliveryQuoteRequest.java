package com.khanh.fooddelivery.delivery_service.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateDeliveryQuoteRequest(@NotNull UUID branchId, @NotNull @Valid DeliveryTargetRequest target) {
    public CreateDeliveryQuoteRequest(UUID branchId, UUID addressId) {
        this(branchId, DeliveryTargetRequest.savedAddress(addressId));
    }
}
