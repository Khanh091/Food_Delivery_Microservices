package com.khanh.fooddelivery.payment_service.service.impl;

import com.khanh.fooddelivery.payment_service.entity.Payout;
import com.khanh.fooddelivery.payment_service.entity.Settlement;
import com.khanh.fooddelivery.payment_service.exception.PaymentException;
import com.khanh.fooddelivery.payment_service.model.LedgerDirection;
import com.khanh.fooddelivery.payment_service.model.LedgerEntryType;
import com.khanh.fooddelivery.payment_service.model.PayoutProvider;
import com.khanh.fooddelivery.payment_service.model.PayoutStatus;
import com.khanh.fooddelivery.payment_service.model.SettlementStatus;
import com.khanh.fooddelivery.payment_service.repository.PayoutRepository;
import com.khanh.fooddelivery.payment_service.repository.SettlementRepository;
import com.khanh.fooddelivery.payment_service.service.LedgerCommand;
import com.khanh.fooddelivery.payment_service.service.LedgerService;
import com.khanh.fooddelivery.payment_service.service.PayoutTransactionService;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PayoutTransactionServiceImpl implements PayoutTransactionService {
    private final SettlementRepository settlements;
    private final PayoutRepository payouts;
    private final LedgerService ledger;
    private final Clock clock;

    @Override
    @Transactional
    public Payout prepare(UUID settlementId) {
        Settlement settlement = settlements.findWithLockById(settlementId)
                .orElseThrow(() -> missing("Settlement not found"));
        Payout existing = payouts.findBySettlementIdForUpdate(settlementId).orElse(null);
        if (existing != null) {
            if (existing.getStatus() == PayoutStatus.FAILED) {
                existing.setStatus(PayoutStatus.PENDING);
                existing.setFailureReason(null);
                settlement.setStatus(SettlementStatus.PROCESSING);
            }
            return existing;
        }
        if (settlement.getStatus() != SettlementStatus.READY
                && settlement.getStatus() != SettlementStatus.PROCESSING) {
            throw conflict("Settlement is not ready for payout");
        }
        if (settlement.getNetAmount().signum() <= 0) {
            throw conflict("Settlement has no payable amount");
        }
        Payout payout = new Payout();
        payout.setId(UUID.randomUUID());
        payout.setSettlementId(settlement.getId());
        payout.setBeneficiaryType(settlement.getBeneficiaryType());
        payout.setBeneficiaryId(settlement.getBeneficiaryId());
        payout.setAmount(settlement.getNetAmount());
        payout.setCurrency("VND");
        payout.setProvider(PayoutProvider.MOCK);
        payout.setStatus(PayoutStatus.PENDING);
        payout.setCreatedAt(Instant.now(clock));
        settlement.setStatus(SettlementStatus.PROCESSING);
        return payouts.save(payout);
    }

    @Override
    @Transactional
    public Payout markSubmitted(UUID payoutId, String providerReference) {
        if (providerReference == null || providerReference.isBlank()) {
            throw invalid("Payout provider reference is required");
        }
        Payout payout = lockPayout(payoutId);
        payout.setProviderReference(providerReference);
        payout.setStatus(PayoutStatus.PROCESSING);
        return payout;
    }

    @Override
    @Transactional
    public Payout markFailed(UUID payoutId, String reason) {
        Payout payout = lockPayout(payoutId);
        payout.setStatus(PayoutStatus.FAILED);
        payout.setFailureReason(reason);
        Settlement settlement = settlements.findWithLockById(payout.getSettlementId())
                .orElseThrow(() -> missing("Settlement not found"));
        settlement.setStatus(SettlementStatus.FAILED);
        return payout;
    }

    @Override
    @Transactional
    public Payout complete(UUID payoutId) {
        Payout payout = lockPayout(payoutId);
        if (payout.getStatus() == PayoutStatus.PAID) {
            return payout;
        }
        if (payout.getStatus() != PayoutStatus.PROCESSING
                && payout.getStatus() != PayoutStatus.PENDING) {
            throw conflict("Payout cannot be completed from " + payout.getStatus());
        }
        if (payout.getProviderReference() == null || payout.getProviderReference().isBlank()) {
            throw conflict("Payout provider confirmation is missing");
        }
        Settlement settlement = settlements.findWithLockById(payout.getSettlementId())
                .orElseThrow(() -> missing("Settlement not found"));
        payout.setStatus(PayoutStatus.PAID);
        payout.setPaidAt(Instant.now(clock));
        settlement.setStatus(SettlementStatus.SETTLED);
        settlement.setPaidAt(payout.getPaidAt());
        LedgerEntryType type = payout.getBeneficiaryType() == com.khanh.fooddelivery.payment_service.model.SettlementBeneficiaryType.RESTAURANT
                ? LedgerEntryType.RESTAURANT_PAYOUT : LedgerEntryType.DRIVER_PAYOUT;
        ledger.record(new LedgerCommand(payout.getBeneficiaryType().name(), payout.getBeneficiaryId(), null, null,
                type, LedgerDirection.DEBIT, payout.getAmount(), payout.getCurrency(),
                "payout:" + payout.getId() + ":paid", payout.getSettlementId(), payout.getId()));
        return payout;
    }

    private Payout lockPayout(UUID payoutId) {
        return payouts.findWithLockById(payoutId).orElseThrow(() -> missing("Payout not found"));
    }

    private PaymentException missing(String message) {
        return new PaymentException("PAYMENT_404", HttpStatus.NOT_FOUND, message);
    }

    private PaymentException conflict(String message) {
        return new PaymentException("PAYMENT_409", HttpStatus.CONFLICT, message);
    }

    private PaymentException invalid(String message) {
        return new PaymentException("PAYMENT_400", HttpStatus.BAD_REQUEST, message);
    }
}
