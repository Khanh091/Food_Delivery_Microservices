package com.khanh.fooddelivery.payment_service.service.impl;

import com.khanh.fooddelivery.payment_service.dto.response.FinanceSummaryResponse;
import com.khanh.fooddelivery.payment_service.entity.LedgerEntry;
import com.khanh.fooddelivery.payment_service.entity.Settlement;
import com.khanh.fooddelivery.payment_service.model.LedgerDirection;
import com.khanh.fooddelivery.payment_service.model.LedgerEntryType;
import com.khanh.fooddelivery.payment_service.model.SettlementStatus;
import com.khanh.fooddelivery.payment_service.repository.SettlementRepository;
import com.khanh.fooddelivery.payment_service.service.FinanceQueryService;
import com.khanh.fooddelivery.payment_service.service.LedgerService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FinanceQueryServiceImpl implements FinanceQueryService {
    private static final int MONEY_SCALE = 2;
    private final LedgerService ledger;
    private final SettlementRepository settlements;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public FinanceSummaryResponse summary(Instant periodFrom, Instant periodTo) {
        Instant to = periodTo == null ? Instant.now(clock) : periodTo;
        Instant from = periodFrom == null ? to.minus(Duration.ofDays(1)) : periodFrom;
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("Finance period must be ordered");
        }
        List<LedgerEntry> entries = ledger.findBetween(from, to);
        BigDecimal restaurantPayable = net(entries, LedgerEntryType.RESTAURANT_PAYABLE_CREATED,
                LedgerEntryType.RESTAURANT_PAYABLE_REVERSAL, LedgerEntryType.RESTAURANT_PAYOUT);
        BigDecimal driverPayable = net(entries, LedgerEntryType.DRIVER_PAYABLE_CREATED,
                LedgerEntryType.DRIVER_PAYABLE_REVERSAL, LedgerEntryType.DRIVER_PAYOUT);
        BigDecimal restaurantReceivable = net(entries, LedgerEntryType.RESTAURANT_COMMISSION_RECEIVABLE);
        BigDecimal driverReceivable = net(entries, LedgerEntryType.DRIVER_COMMISSION_RECEIVABLE);
        BigDecimal platformRevenue = net(entries, LedgerEntryType.PLATFORM_REVENUE_RECOGNIZED,
                LedgerEntryType.PLATFORM_REVENUE_REVERSAL);
        BigDecimal settledAmount = positive(entries, LedgerEntryType.RESTAURANT_PAYOUT)
                .add(positive(entries, LedgerEntryType.DRIVER_PAYOUT));
        long ready = settlements.findAll().stream()
                .filter(value -> value.getStatus() == SettlementStatus.READY)
                .filter(value -> overlaps(value, from, to))
                .count();
        long settled = settlements.findAll().stream()
                .filter(value -> value.getStatus() == SettlementStatus.SETTLED)
                .filter(value -> overlaps(value, from, to))
                .count();
        return new FinanceSummaryResponse(from, to, restaurantPayable, driverPayable,
                restaurantReceivable, driverReceivable, platformRevenue,
                restaurantPayable.add(driverPayable), settledAmount, ready, settled);
    }

    private boolean overlaps(Settlement settlement, Instant from, Instant to) {
        return settlement.getPeriodTo().isAfter(from) && settlement.getPeriodFrom().isBefore(to);
    }

    private BigDecimal positive(List<LedgerEntry> entries, LedgerEntryType type) {
        return entries.stream().filter(entry -> entry.getEntryType() == type)
                .map(LedgerEntry::getAmount).reduce(zero(), BigDecimal::add).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal net(List<LedgerEntry> entries, LedgerEntryType... types) {
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
        return total.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal zero() {
        return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
