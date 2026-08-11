package com.khanh.fooddelivery.catalog_service.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record BranchItemPriceUpdateRequest(
        @NotNull @DecimalMin("0.00") BigDecimal sellingPrice,
        @DecimalMin("0.00") BigDecimal originalPrice,
        @Size(max = 500) String reason) {}
