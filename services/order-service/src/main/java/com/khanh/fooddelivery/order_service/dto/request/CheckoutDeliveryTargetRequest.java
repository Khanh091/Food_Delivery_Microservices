package com.khanh.fooddelivery.order_service.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CheckoutDeliveryTargetRequest(@NotBlank String type, UUID addressId, UUID temporaryLocationId) {
    public static CheckoutDeliveryTargetRequest savedAddress(UUID addressId) {
        return new CheckoutDeliveryTargetRequest("SAVED_ADDRESS", addressId, null);
    }

    @AssertTrue(message = "Delivery target is invalid")
    public boolean isValid() {
        return "SAVED_ADDRESS".equals(type) ? addressId != null && temporaryLocationId == null
                : "TEMPORARY_LOCATION".equals(type) && addressId == null && temporaryLocationId != null;
    }
}
