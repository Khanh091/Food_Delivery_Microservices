package com.khanh.fooddelivery.order_service.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CheckoutPreviewRequest(
        @NotNull UUID branchId,
        @Min(1) long cartVersion,
        @NotNull @Valid CheckoutDeliveryTargetRequest target
) {

    public CheckoutPreviewRequest(
            UUID branchId,
            long cartVersion,
            UUID addressId
    ) {
        this(
                branchId,
                cartVersion,
                CheckoutDeliveryTargetRequest.savedAddress(addressId)
        );
    }
}