package com.khanh.fooddelivery.cart_service.dto.response.internal;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InternalCartSnapshotResponse(
        UUID ownerUserId,
        UUID restaurantId,
        UUID branchId,
        String currency,
        List<InternalCartItemSnapshotResponse> items,
        long version,
        Instant createdAt,
        Instant updatedAt) {}
