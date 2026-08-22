package com.khanh.fooddelivery.payment_service.dto.response;

import com.khanh.fooddelivery.payment_service.model.PayoutProvider;
import com.khanh.fooddelivery.payment_service.model.PayoutStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PayoutResponse(UUID id, UUID settlementId, UUID beneficiaryId, BigDecimal amount,
                             String currency, PayoutStatus status, PayoutProvider provider,
                             String providerReference, Instant paidAt) {
}
