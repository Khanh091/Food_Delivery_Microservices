package com.khanh.fooddelivery.catalog_service.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record BranchItemCreateRequest(
        @NotNull UUID itemId,
        @NotNull UUID branchId,
        @NotNull @DecimalMin("0.00") BigDecimal sellingPrice,
        @DecimalMin("0.00") BigDecimal originalPrice,
        @Min(0) Integer availableQuantity) {}
