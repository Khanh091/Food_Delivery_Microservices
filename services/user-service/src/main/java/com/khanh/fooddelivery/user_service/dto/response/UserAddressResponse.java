package com.khanh.fooddelivery.user_service.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record UserAddressResponse(
        UUID id,
        String label,
        String recipientName,
        String recipientPhone,
        String addressLine,
        String ward,
        String district,
        String city,
        BigDecimal latitude,
        BigDecimal longitude,
        String deliveryNote,
        boolean isDefault,
        Instant createdAt,
        Instant updatedAt
) {
}
