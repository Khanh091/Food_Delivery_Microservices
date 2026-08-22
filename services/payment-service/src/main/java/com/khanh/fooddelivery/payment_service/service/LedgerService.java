package com.khanh.fooddelivery.payment_service.service;

import com.khanh.fooddelivery.payment_service.entity.LedgerEntry;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface LedgerService {
    LedgerEntry record(LedgerCommand command);

    List<LedgerEntry> findBetween(Instant periodFrom, Instant periodTo);

    List<LedgerEntry> findUnsettled(String ownerType, UUID ownerId, Instant periodFrom, Instant periodTo);

    void claimForSettlement(List<LedgerEntry> entries, UUID settlementId);
}
