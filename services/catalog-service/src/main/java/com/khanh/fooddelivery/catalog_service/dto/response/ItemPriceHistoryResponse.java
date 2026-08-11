package com.khanh.fooddelivery.catalog_service.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ItemPriceHistoryResponse(
        UUID id,
        BigDecimal oldPrice,
        BigDecimal newPrice,
        String reason,
        UUID changedBy,
        Instant createdAt) {}
