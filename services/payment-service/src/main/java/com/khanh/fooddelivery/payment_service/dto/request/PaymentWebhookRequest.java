package com.khanh.fooddelivery.payment_service.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentWebhookRequest(
        UUID paymentId,
        UUID orderId,
        String providerTransactionId,
        String status,
        BigDecimal amount,
        String currency,
        String signature
) {
}
