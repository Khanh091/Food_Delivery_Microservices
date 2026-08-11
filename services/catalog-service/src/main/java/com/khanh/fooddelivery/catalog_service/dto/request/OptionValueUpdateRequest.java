package com.khanh.fooddelivery.catalog_service.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record OptionValueUpdateRequest(
        @Size(max = 255) String name,
        @DecimalMin("0.00") BigDecimal additionalPrice,
        @jakarta.validation.constraints.Min(0) Integer sortOrder) {}
