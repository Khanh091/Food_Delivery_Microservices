package com.khanh.fooddelivery.catalog_service.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OptionValueResponse(
        UUID id,
        UUID optionGroupId,
        String name,
        BigDecimal additionalPrice,
        Boolean isAvailable,
        Integer sortOrder,
        Instant createdAt,
        Instant updatedAt) {}
