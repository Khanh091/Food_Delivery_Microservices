package com.khanh.fooddelivery.delivery_service.dto.response;

import com.khanh.fooddelivery.delivery_service.model.DeliveryOfferStatus;
import java.time.Instant;
import java.util.UUID;

public record DeliveryOfferResponse(
        UUID id,
        Long version,
        UUID deliveryId,
        UUID driverId,
        DeliveryOfferStatus status,
        Instant offeredAt,
        Instant expiresAt,
        Instant respondedAt
) {
}
