package com.khanh.fooddelivery.payment_service.dto.response;

import com.khanh.fooddelivery.payment_service.model.FeePolicyStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FeePolicyResponse(UUID id, Integer policyVersion, BigDecimal restaurantCommissionRate,
                                BigDecimal driverCommissionRate, Instant effectiveFrom,
                                Instant effectiveTo, FeePolicyStatus status) {
}
