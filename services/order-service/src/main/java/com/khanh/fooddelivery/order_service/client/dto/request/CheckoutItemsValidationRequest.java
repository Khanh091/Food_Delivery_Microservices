package com.khanh.fooddelivery.order_service.client.dto.request;

import java.util.List;
import java.util.UUID;

public record CheckoutItemsValidationRequest(
        UUID restaurantId,
        UUID branchId,
        List<CheckoutItemRequest> items
) {
}
