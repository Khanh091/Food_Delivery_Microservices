package com.khanh.fooddelivery.order_service.client.dto.request;

import java.util.List;
import java.util.UUID;

public record CheckoutItemRequest(
        UUID cartItemId,
        UUID catalogItemId,
        List<UUID> selectedOptionValueIds
) {
}
