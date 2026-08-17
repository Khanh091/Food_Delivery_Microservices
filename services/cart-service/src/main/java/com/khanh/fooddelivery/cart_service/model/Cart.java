package com.khanh.fooddelivery.cart_service.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Cart(
        int schemaVersion,
        UUID ownerUserId,
        UUID restaurantId,
        UUID branchId,
        String restaurantNameSnapshot,
        String branchNameSnapshot,
        String currency,
        List<CartItem> items,
        long version,
        Instant createdAt,
        Instant updatedAt) {}
