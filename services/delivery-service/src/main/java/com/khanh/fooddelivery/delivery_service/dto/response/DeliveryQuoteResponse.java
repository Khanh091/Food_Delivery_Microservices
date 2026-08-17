package com.khanh.fooddelivery.delivery_service.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DeliveryQuoteResponse(
        UUID quoteId, boolean serviceable, String currency, BigDecimal deliveryFee, long distanceMeters,
        long estimatedDurationMinutes, String pricingPolicyVersion, Instant calculatedAt, Instant expiresAt) {}
