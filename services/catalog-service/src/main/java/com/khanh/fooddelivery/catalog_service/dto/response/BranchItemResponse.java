package com.khanh.fooddelivery.catalog_service.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BranchItemResponse(
        UUID id,
        UUID itemId,
        UUID branchId,
        BigDecimal sellingPrice,
        BigDecimal originalPrice,
        Boolean isAvailable,
        Integer availableQuantity,
        Instant soldOutUntil,
        Instant createdAt,
        Instant updatedAt) {}
