package com.khanh.fooddelivery.cart_service.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CartSummaryResponse(
        UUID restaurantId,
        String restaurantName,
        UUID branchId,
        String branchName,
        int totalQuantity,
        BigDecimal subtotal,
        String currency,
        long version,
        Instant updatedAt,
        Instant expiresAt) {}
