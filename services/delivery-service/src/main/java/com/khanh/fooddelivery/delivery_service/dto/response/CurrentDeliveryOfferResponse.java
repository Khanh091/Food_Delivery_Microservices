package com.khanh.fooddelivery.delivery_service.dto.response;

import com.khanh.fooddelivery.delivery_service.model.DeliveryStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * The authoritative offer projection used by a driver's current-offer view.
 * It deliberately contains no customer contact or payment information.
 */
public record CurrentDeliveryOfferResponse(
        UUID offerId,
        UUID deliveryId,
        Instant offeredAt,
        Instant expiresAt,
        DeliveryStatus deliveryStatus,
        String restaurantName,
        String branchName,
        BigDecimal pickupLatitude,
        BigDecimal pickupLongitude,
        String customerAddress
) {
}
