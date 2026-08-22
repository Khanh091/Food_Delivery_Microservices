package com.khanh.fooddelivery.payment_service.dto.response;

import com.khanh.fooddelivery.payment_service.model.PaymentMethod;
import com.khanh.fooddelivery.payment_service.model.PaymentStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record FinancialFactsResponse(
        UUID paymentId,
        UUID orderId,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        String currency,
        BigDecimal foodGrossAmount,
        BigDecimal deliveryGrossAmount,
        BigDecimal customerPayableAmount,
        BigDecimal requiredRestaurantAdvance,
        BigDecimal customerCashToCollect,
        BigDecimal driverGrossEarning,
        BigDecimal restaurantCommissionAmount,
        BigDecimal driverCommissionAmount,
        BigDecimal driverNetEarning,
        BigDecimal restaurantNetAmount,
        BigDecimal platformRevenueAmount,
        UUID feePolicyId,
        Integer feePolicyVersion,
        boolean restaurantAdvanceConfirmed,
        boolean customerCashCollected
) {
}
