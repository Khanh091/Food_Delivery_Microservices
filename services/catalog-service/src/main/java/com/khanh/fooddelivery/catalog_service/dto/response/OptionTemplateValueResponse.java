package com.khanh.fooddelivery.catalog_service.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record OptionTemplateValueResponse(
        UUID id,
        String name,
        BigDecimal additionalPrice,
        Boolean isAvailable,
        Integer sortOrder) {}
