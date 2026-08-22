package com.khanh.fooddelivery.payment_service.service;

import com.khanh.fooddelivery.payment_service.entity.Payout;
import java.util.UUID;

public interface PayoutTransactionService {
    Payout prepare(UUID settlementId);

    Payout markSubmitted(UUID payoutId, String providerReference);

    Payout markFailed(UUID payoutId, String reason);

    Payout complete(UUID payoutId);
}
