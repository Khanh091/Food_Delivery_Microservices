package com.khanh.fooddelivery.order_service.client.dto.response;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
@JsonIgnoreProperties(ignoreUnknown = true)
public record DeliveryQuoteResponse(
        UUID quoteId,
        boolean serviceable,
        String currency,
        BigDecimal deliveryFee,
        long distanceMeters,
        long estimatedDurationMinutes,
        String pricingPolicyVersion,
        Instant calculatedAt,
        Instant expiresAt
) {
}
