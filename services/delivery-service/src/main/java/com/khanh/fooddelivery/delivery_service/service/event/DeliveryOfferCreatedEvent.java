package com.khanh.fooddelivery.delivery_service.service.event;

import java.util.UUID;

public record DeliveryOfferCreatedEvent(
        UUID driverId,
        UUID offerId,
        UUID deliveryId
) {
}
