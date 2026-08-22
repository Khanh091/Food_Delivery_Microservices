package com.khanh.fooddelivery.delivery_service.client.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record FinancialFactsResponse(UUID paymentId, UUID orderId, String paymentMethod, String paymentStatus,
                                     String currency, BigDecimal foodGrossAmount, BigDecimal deliveryGrossAmount,
                                     BigDecimal customerPayableAmount, BigDecimal requiredRestaurantAdvance,
                                     BigDecimal customerCashToCollect, BigDecimal driverGrossEarning,
                                     BigDecimal restaurantCommissionAmount, BigDecimal driverCommissionAmount,
                                     BigDecimal driverNetEarning, BigDecimal restaurantNetAmount,
                                     BigDecimal platformRevenueAmount, UUID feePolicyId, Integer feePolicyVersion,
                                     boolean restaurantAdvanceConfirmed, boolean customerCashCollected) {
}
