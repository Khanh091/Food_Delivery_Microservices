package com.khanh.fooddelivery.delivery_service.dto.request;

import com.khanh.fooddelivery.delivery_service.model.DeliveryTargetType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record DeliveryTargetRequest(@NotNull DeliveryTargetType type, UUID addressId, UUID temporaryLocationId) {
    public static DeliveryTargetRequest savedAddress(UUID addressId) {
        return new DeliveryTargetRequest(DeliveryTargetType.SAVED_ADDRESS, addressId, null);
    }

    @AssertTrue(message = "Delivery target is invalid")
    public boolean isValid() {
        return type == DeliveryTargetType.SAVED_ADDRESS ? addressId != null && temporaryLocationId == null
                : type == DeliveryTargetType.TEMPORARY_LOCATION && addressId == null && temporaryLocationId != null;
    }
}
