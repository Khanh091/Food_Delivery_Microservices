package com.khanh.fooddelivery.order_service.client.dto.request;

import java.util.UUID;

public record DeliveryTargetRequest(String type, UUID addressId, UUID temporaryLocationId) {

    public static DeliveryTargetRequest savedAddress(UUID addressId) {
        return new DeliveryTargetRequest("SAVED_ADDRESS", addressId, null);
    }
}
