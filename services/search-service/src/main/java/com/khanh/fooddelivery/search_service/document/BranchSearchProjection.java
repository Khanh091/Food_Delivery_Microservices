package com.khanh.fooddelivery.search_service.document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BranchSearchProjection(
        UUID branchItemId,
        UUID branchId,
        BigDecimal sellingPrice,
        BigDecimal originalPrice,
        boolean isAvailable,
        Integer availableQuantity,
        Instant soldOutUntil,
        long aggregateVersion,
        UUID lastEventId) {}
