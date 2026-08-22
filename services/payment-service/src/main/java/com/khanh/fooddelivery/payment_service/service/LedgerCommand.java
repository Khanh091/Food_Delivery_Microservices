package com.khanh.fooddelivery.payment_service.service;

import com.khanh.fooddelivery.payment_service.model.LedgerDirection;
import com.khanh.fooddelivery.payment_service.model.LedgerEntryType;
import java.math.BigDecimal;
import java.util.UUID;

public record LedgerCommand(
        String ownerType,
        UUID ownerId,
        UUID orderId,
        UUID paymentId,
        LedgerEntryType entryType,
        LedgerDirection direction,
        BigDecimal amount,
        String currency,
        String idempotencyReference,
        UUID settlementId,
        UUID payoutId
) {
    public LedgerCommand(String ownerType, UUID ownerId, UUID orderId, UUID paymentId,
                         LedgerEntryType entryType, LedgerDirection direction, BigDecimal amount,
                         String currency, String idempotencyReference) {
        this(ownerType, ownerId, orderId, paymentId, entryType, direction, amount, currency,
                idempotencyReference, null, null);
    }
}
