package com.khanh.fooddelivery.catalog_service.dto.response.publiccatalog;

import java.math.BigDecimal;
import java.util.UUID;

public record PublicOptionValueResponse(
        UUID id, String name, BigDecimal additionalPrice, Integer sortOrder) {}
