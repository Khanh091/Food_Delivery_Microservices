package com.khanh.fooddelivery.delivery_service.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CheckoutTemporaryLocation(
        UUID id, UUID ownerUserId, UUID branchId, String formattedAddress, String addressLine,
        String ward, String district, String city, BigDecimal latitude, BigDecimal longitude,
        Instant createdAt, Instant updatedAt, Instant expiresAt) {}
