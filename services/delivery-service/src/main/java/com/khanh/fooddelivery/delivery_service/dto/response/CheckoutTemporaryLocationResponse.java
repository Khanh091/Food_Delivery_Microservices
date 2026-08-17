package com.khanh.fooddelivery.delivery_service.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CheckoutTemporaryLocationResponse(
        UUID id, UUID branchId, String formattedAddress, String addressLine, String ward,
        String district, String city, BigDecimal latitude, BigDecimal longitude, Instant expiresAt) {}
