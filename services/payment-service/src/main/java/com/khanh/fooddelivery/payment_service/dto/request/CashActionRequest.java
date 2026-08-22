package com.khanh.fooddelivery.payment_service.dto.request;

import java.util.UUID;

public record CashActionRequest(UUID orderId, UUID deliveryId, UUID driverId, String idempotencyKey) {
}
