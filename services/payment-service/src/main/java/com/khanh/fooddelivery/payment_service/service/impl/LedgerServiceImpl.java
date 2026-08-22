package com.khanh.fooddelivery.payment_service.service.impl;

import com.khanh.fooddelivery.payment_service.entity.LedgerEntry;
import com.khanh.fooddelivery.payment_service.exception.PaymentException;
import com.khanh.fooddelivery.payment_service.repository.LedgerEntryRepository;
import com.khanh.fooddelivery.payment_service.service.LedgerCommand;
import com.khanh.fooddelivery.payment_service.service.LedgerService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LedgerServiceImpl implements LedgerService {
    private static final int MONEY_SCALE = 2;

    private final LedgerEntryRepository repository;
    private final Clock clock;

    @Override
    @Transactional
    public LedgerEntry record(LedgerCommand command) {
        if (command == null || command.entryType() == null || command.direction() == null
                || command.amount() == null || command.currency() == null || command.currency().isBlank()
                || command.idempotencyReference() == null || command.idempotencyReference().isBlank()) {
            throw new PaymentException("PAYMENT_400", HttpStatus.BAD_REQUEST, "Ledger entry fields are required");
        }
        LedgerEntry existing = repository.findByIdempotencyReference(command.idempotencyReference()).orElse(null);
        if (existing != null) {
            return existing;
        }

        BigDecimal amount = money(command.amount());
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Ledger amount cannot be negative");
        }
        LedgerEntry entry = new LedgerEntry();
        entry.setId(UUID.randomUUID());
        entry.setOwnerType(command.ownerType());
        entry.setOwnerId(command.ownerId());
        entry.setOrderId(command.orderId());
        entry.setPaymentId(command.paymentId());
        entry.setSettlementId(command.settlementId());
        entry.setPayoutId(command.payoutId());
        entry.setEntryType(command.entryType());
        entry.setDirection(command.direction());
        entry.setAmount(amount);
        entry.setCurrency(command.currency());
        entry.setIdempotencyReference(command.idempotencyReference());
        Instant now = Instant.now(clock);
        entry.setOccurredAt(now);
        entry.setCreatedAt(now);
        return repository.save(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LedgerEntry> findBetween(Instant periodFrom, Instant periodTo) {
        return repository.findByOccurredAtGreaterThanEqualAndOccurredAtLessThan(periodFrom, periodTo);
    }

    @Override
    @Transactional
    public List<LedgerEntry> findUnsettled(String ownerType, UUID ownerId, Instant periodFrom, Instant periodTo) {
        return repository.findUnsettledByOwnerAndPeriod(ownerType, ownerId, periodFrom, periodTo);
    }

    @Override
    @Transactional
    public void claimForSettlement(List<LedgerEntry> entries, UUID settlementId) {
        entries.forEach(entry -> {
            if (entry.getSettlementId() != null && !entry.getSettlementId().equals(settlementId)) {
                throw new IllegalStateException("Ledger entry is already claimed by another settlement");
            }
            entry.setSettlementId(settlementId);
        });
        repository.saveAll(entries);
    }

    private BigDecimal money(BigDecimal amount) {
        return (amount == null ? BigDecimal.ZERO : amount).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
