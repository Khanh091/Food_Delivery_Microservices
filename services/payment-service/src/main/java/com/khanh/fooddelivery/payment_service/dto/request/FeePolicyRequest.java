package com.khanh.fooddelivery.payment_service.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public record FeePolicyRequest(
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal restaurantCommissionRate,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal driverCommissionRate,
        @NotNull Instant effectiveFrom
) {
}
