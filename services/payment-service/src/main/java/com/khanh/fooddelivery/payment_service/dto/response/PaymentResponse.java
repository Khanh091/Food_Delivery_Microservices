package com.khanh.fooddelivery.payment_service.dto.response;

import com.khanh.fooddelivery.payment_service.model.PaymentMethod;
import com.khanh.fooddelivery.payment_service.model.PaymentProvider;
import com.khanh.fooddelivery.payment_service.model.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(UUID id, UUID orderId, PaymentMethod method, PaymentStatus status,
                             BigDecimal amount, String currency, PaymentProvider provider,
                             String providerTransactionId, String providerReference,
                             Instant paidAt, Instant collectedAt, Instant refundedAt,
                             UUID feePolicyId, Integer feePolicyVersion,
                             BigDecimal restaurantCommissionAmount, BigDecimal restaurantNetAmount,
                             BigDecimal driverCommissionAmount, BigDecimal driverNetAmount,
                             BigDecimal platformRevenueAmount) {
}
