package com.khanh.fooddelivery.payment_service.service;

import java.math.BigDecimal;
import java.util.UUID;

public record FinancialBreakdown(
        UUID feePolicyId,
        Integer feePolicyVersion,
        BigDecimal foodGrossAmount,
        BigDecimal deliveryGrossAmount,
        BigDecimal restaurantCommissionRate,
        BigDecimal restaurantCommissionAmount,
        BigDecimal restaurantNetAmount,
        BigDecimal driverCommissionRate,
        BigDecimal driverCommissionAmount,
        BigDecimal driverNetAmount,
        BigDecimal platformRevenueAmount,
        BigDecimal customerPayableAmount,
        BigDecimal paymentProcessingFee,
        String currency
) {
}
