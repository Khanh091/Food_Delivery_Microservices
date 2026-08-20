package com.khanh.fooddelivery.order_service.client.dto.response;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
@JsonIgnoreProperties(ignoreUnknown = true)
public record CheckoutTemporaryLocationResponse(
        UUID id,
        UUID branchId,
        String formattedAddress,
        String addressLine,
        String ward,
        String district,
        String city,
        BigDecimal latitude,
        BigDecimal longitude,
        Instant expiresAt
) {
}
