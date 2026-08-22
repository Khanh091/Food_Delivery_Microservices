package com.khanh.fooddelivery.payment_service.service.impl;

import com.khanh.fooddelivery.payment_service.dto.request.SettlementRequest;
import com.khanh.fooddelivery.payment_service.dto.response.SettlementResponse;
import com.khanh.fooddelivery.payment_service.entity.LedgerEntry;
import com.khanh.fooddelivery.payment_service.entity.Settlement;
import com.khanh.fooddelivery.payment_service.exception.PaymentException;
import com.khanh.fooddelivery.payment_service.mapper.SettlementMapper;
import com.khanh.fooddelivery.payment_service.model.LedgerDirection;
import com.khanh.fooddelivery.payment_service.model.LedgerEntryType;
import com.khanh.fooddelivery.payment_service.model.SettlementBeneficiaryType;
import com.khanh.fooddelivery.payment_service.model.SettlementStatus;
import com.khanh.fooddelivery.payment_service.repository.SettlementRepository;
import com.khanh.fooddelivery.payment_service.service.LedgerService;
import com.khanh.fooddelivery.payment_service.service.SettlementService;
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
public class SettlementServiceImpl implements SettlementService {
    private static final int MONEY_SCALE = 2;

    private final SettlementRepository settlements;
    private final LedgerService ledger;
    private final SettlementMapper mapper;
    private final Clock clock;

    @Override
    @Transactional
    public SettlementResponse create(SettlementRequest request) {
        validate(request);
        Settlement existing = settlements.findByBeneficiaryTypeAndBeneficiaryIdAndPeriodFromAndPeriodTo(
                request.beneficiaryType(), request.beneficiaryId(), request.periodFrom(), request.periodTo()).orElse(null);
        if (existing != null) {
            return mapper.toResponse(existing);
        }

        String ownerType = request.beneficiaryType().name();
        List<LedgerEntry> entries = ledger.findUnsettled(ownerType, request.beneficiaryId(),
                        request.periodFrom(), request.periodTo()).stream()
                .filter(entry -> eligible(request.beneficiaryType(), entry.getEntryType()))
                .toList();
        Settlement settlement = new Settlement();
        settlement.setId(UUID.randomUUID());
        settlement.setBeneficiaryType(request.beneficiaryType());
        settlement.setBeneficiaryId(request.beneficiaryId());
        settlement.setPeriodFrom(request.periodFrom());
        settlement.setPeriodTo(request.periodTo());
        BigDecimal gross = signedSum(entries, payableType(request.beneficiaryType()),
                reversalType(request.beneficiaryType()));
        BigDecimal commission = signedSum(entries, commissionType(request.beneficiaryType()));
        settlement.setGrossAmount(gross);
        settlement.setCommissionAmount(commission);
        settlement.setAdjustmentAmount(zero());
        settlement.setNetAmount(money(gross.subtract(commission)));
        settlement.setStatus(SettlementStatus.READY);
        Instant now = Instant.now(clock);
        settlement.setCreatedAt(now);
        settlement.setFinalizedAt(now);
        ledger.claimForSettlement(entries, settlement.getId());
        return mapper.toResponse(settlements.save(settlement));
    }

    private boolean eligible(SettlementBeneficiaryType beneficiary, LedgerEntryType type) {
        if (beneficiary == SettlementBeneficiaryType.RESTAURANT) {
            return type == LedgerEntryType.RESTAURANT_PAYABLE_CREATED
                    || type == LedgerEntryType.RESTAURANT_PAYABLE_REVERSAL
                    || type == LedgerEntryType.RESTAURANT_COMMISSION_RECEIVABLE;
        }
        return type == LedgerEntryType.DRIVER_PAYABLE_CREATED
                || type == LedgerEntryType.DRIVER_PAYABLE_REVERSAL
                || type == LedgerEntryType.DRIVER_COMMISSION_RECEIVABLE;
    }

    private LedgerEntryType payableType(SettlementBeneficiaryType beneficiary) {
        return beneficiary == SettlementBeneficiaryType.RESTAURANT
                ? LedgerEntryType.RESTAURANT_PAYABLE_CREATED : LedgerEntryType.DRIVER_PAYABLE_CREATED;
    }

    private LedgerEntryType reversalType(SettlementBeneficiaryType beneficiary) {
        return beneficiary == SettlementBeneficiaryType.RESTAURANT
                ? LedgerEntryType.RESTAURANT_PAYABLE_REVERSAL : LedgerEntryType.DRIVER_PAYABLE_REVERSAL;
    }

    private LedgerEntryType commissionType(SettlementBeneficiaryType beneficiary) {
        return beneficiary == SettlementBeneficiaryType.RESTAURANT
                ? LedgerEntryType.RESTAURANT_COMMISSION_RECEIVABLE : LedgerEntryType.DRIVER_COMMISSION_RECEIVABLE;
    }

    private BigDecimal signedSum(List<LedgerEntry> entries, LedgerEntryType... types) {
        BigDecimal total = zero();
        for (LedgerEntry entry : entries) {
            for (LedgerEntryType type : types) {
                if (entry.getEntryType() == type) {
                    total = entry.getDirection() == LedgerDirection.CREDIT
                            ? total.add(entry.getAmount()) : total.subtract(entry.getAmount());
                    break;
                }
            }
        }
        return money(total);
    }

    private void validate(SettlementRequest request) {
        if (request == null || request.beneficiaryType() == null || request.beneficiaryId() == null
                || request.periodFrom() == null || request.periodTo() == null
                || !request.periodFrom().isBefore(request.periodTo())) {
            throw new PaymentException("PAYMENT_400", HttpStatus.BAD_REQUEST,
                    "A valid settlement beneficiary and period are required");
        }
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? zero() : value).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal zero() {
        return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
