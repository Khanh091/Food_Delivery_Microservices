package com.khanh.fooddelivery.order_service.client.dto.response;

import com.khanh.fooddelivery.order_service.enums.PaymentMethod;
import com.khanh.fooddelivery.order_service.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(UUID id, UUID orderId, PaymentMethod method, PaymentStatus status,
                              BigDecimal amount, String currency, String provider,
                              String providerTransactionId, String providerReference,
                              Instant paidAt, Instant collectedAt, Instant refundedAt,
                              UUID feePolicyId, Integer feePolicyVersion,
                              BigDecimal restaurantCommissionAmount, BigDecimal restaurantNetAmount,
                              BigDecimal driverCommissionAmount, BigDecimal driverNetAmount,
                              BigDecimal platformRevenueAmount) {
}
