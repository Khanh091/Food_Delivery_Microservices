package com.khanh.fooddelivery.payment_service.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record FinanceSummaryResponse(
        Instant periodFrom,
        Instant periodTo,
        BigDecimal restaurantPayable,
        BigDecimal driverPayable,
        BigDecimal restaurantReceivable,
        BigDecimal driverReceivable,
        BigDecimal platformRevenue,
        BigDecimal payoutLiability,
        BigDecimal settledAmount,
        long readySettlementCount,
        long settledSettlementCount
) {
}
