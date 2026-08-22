package com.khanh.fooddelivery.delivery_service.client.dto.request;

import java.util.UUID;

public record CashActionRequest(UUID orderId, UUID deliveryId, UUID driverId, String idempotencyKey) {
}
