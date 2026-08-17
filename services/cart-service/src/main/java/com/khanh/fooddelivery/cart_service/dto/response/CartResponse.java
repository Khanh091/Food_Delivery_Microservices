package com.khanh.fooddelivery.cart_service.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID restaurantId,
        String restaurantName,
        UUID branchId,
        String branchName,
        String currency,
        List<CartItemResponse> items,
        BigDecimal subtotal,
        int totalQuantity,
        long version,
        Instant createdAt,
        Instant updatedAt,
        Instant expiresAt) {}
