package com.khanh.fooddelivery.order_service.client.dto.request;

import java.util.UUID;

public record DeliveryQuoteRequest(UUID branchId, DeliveryTargetRequest target) {

    public DeliveryQuoteRequest(UUID branchId, UUID addressId) {
        this(branchId, DeliveryTargetRequest.savedAddress(addressId));
    }
}
