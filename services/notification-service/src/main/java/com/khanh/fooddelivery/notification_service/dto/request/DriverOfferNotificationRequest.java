package com.khanh.fooddelivery.notification_service.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record DriverOfferNotificationRequest(
        @NotNull UUID driverId,
        @NotNull UUID offerId,
        @NotNull UUID deliveryId
) {
}
