package com.khanh.fooddelivery.delivery_service.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DeliveryQuote(
        UUID quoteId, UUID ownerUserId, UUID branchId, DeliveryTargetType targetType, UUID addressId, UUID temporaryLocationId,
        String currency, BigDecimal deliveryFee,
        long distanceMeters, long estimatedDurationMinutes, String pricingPolicyVersion, Instant calculatedAt,
        Instant expiresAt) {}
