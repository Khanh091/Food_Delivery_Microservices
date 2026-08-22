package com.khanh.fooddelivery.payment_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.payment_service.dto.request.SettlementRequest;
import com.khanh.fooddelivery.payment_service.entity.LedgerEntry;
import com.khanh.fooddelivery.payment_service.entity.Settlement;
import com.khanh.fooddelivery.payment_service.mapper.SettlementMapper;
import com.khanh.fooddelivery.payment_service.model.LedgerDirection;
import com.khanh.fooddelivery.payment_service.model.LedgerEntryType;
import com.khanh.fooddelivery.payment_service.model.SettlementBeneficiaryType;
import com.khanh.fooddelivery.payment_service.repository.SettlementRepository;
import com.khanh.fooddelivery.payment_service.service.impl.SettlementServiceImpl;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class SettlementServiceTests {
    private final SettlementRepository settlements = Mockito.mock(SettlementRepository.class);
    private final LedgerService ledger = Mockito.mock(LedgerService.class);
    private final SettlementServiceImpl service = new SettlementServiceImpl(settlements, ledger,
            Mappers.getMapper(SettlementMapper.class),
            Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));

    @Test
    void codRestaurantCommissionBecomesNegativeDebtAndCashMovementIsNotSettled() {
        UUID owner = UUID.randomUUID();
        Instant from = Instant.parse("2025-12-31T00:00:00Z");
        Instant to = Instant.parse("2026-01-02T00:00:00Z");
        LedgerEntry advance = entry(LedgerEntryType.DRIVER_RESTAURANT_ADVANCE, "50000", LedgerDirection.DEBIT);
        LedgerEntry cash = entry(LedgerEntryType.DRIVER_CUSTOMER_CASH_COLLECTED, "75000", LedgerDirection.CREDIT);
        LedgerEntry commission = entry(LedgerEntryType.RESTAURANT_COMMISSION_RECEIVABLE, "15000", LedgerDirection.CREDIT);
        when(settlements.findByBeneficiaryTypeAndBeneficiaryIdAndPeriodFromAndPeriodTo(
                SettlementBeneficiaryType.RESTAURANT, owner, from, to)).thenReturn(Optional.empty());
        when(ledger.findUnsettled("RESTAURANT", owner, from, to)).thenReturn(List.of(advance, cash, commission));
        when(settlements.save(any(Settlement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(new SettlementRequest(SettlementBeneficiaryType.RESTAURANT, owner, from, to));

        assertThat(response.grossAmount()).isEqualByComparingTo("0.00");
        assertThat(response.commissionAmount()).isEqualByComparingTo("15000.00");
        assertThat(response.netAmount()).isEqualByComparingTo("-15000.00");
        ArgumentCaptor<List<LedgerEntry>> claimed = ArgumentCaptor.forClass(List.class);
        Mockito.verify(ledger).claimForSettlement(claimed.capture(), any(UUID.class));
        assertThat(claimed.getValue()).containsExactly(commission);
    }

    @Test
    void onlinePayableUsesNetAmountWithoutDoubleCountingCommission() {
        UUID owner = UUID.randomUUID();
        Instant from = Instant.parse("2025-12-31T00:00:00Z");
        Instant to = Instant.parse("2026-01-02T00:00:00Z");
        LedgerEntry payable = entry(LedgerEntryType.RESTAURANT_PAYABLE_CREATED, "35000", LedgerDirection.CREDIT);
        when(settlements.findByBeneficiaryTypeAndBeneficiaryIdAndPeriodFromAndPeriodTo(
                SettlementBeneficiaryType.RESTAURANT, owner, from, to)).thenReturn(Optional.empty());
        when(ledger.findUnsettled("RESTAURANT", owner, from, to)).thenReturn(List.of(payable));
        when(settlements.save(any(Settlement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(new SettlementRequest(SettlementBeneficiaryType.RESTAURANT, owner, from, to));

        assertThat(response.grossAmount()).isEqualByComparingTo("35000.00");
        assertThat(response.commissionAmount()).isEqualByComparingTo("0.00");
        assertThat(response.netAmount()).isEqualByComparingTo("35000.00");
    }

    @Test
    void codDriverCommissionBecomesNegativeDebt() {
        UUID owner = UUID.randomUUID();
        Instant from = Instant.parse("2025-12-31T00:00:00Z");
        Instant to = Instant.parse("2026-01-02T00:00:00Z");
        LedgerEntry commission = entry(LedgerEntryType.DRIVER_COMMISSION_RECEIVABLE, "7500", LedgerDirection.CREDIT);
        when(settlements.findByBeneficiaryTypeAndBeneficiaryIdAndPeriodFromAndPeriodTo(
                SettlementBeneficiaryType.DRIVER, owner, from, to)).thenReturn(Optional.empty());
        when(ledger.findUnsettled("DRIVER", owner, from, to)).thenReturn(List.of(commission));
        when(settlements.save(any(Settlement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(new SettlementRequest(SettlementBeneficiaryType.DRIVER, owner, from, to));

        assertThat(response.grossAmount()).isEqualByComparingTo("0.00");
        assertThat(response.commissionAmount()).isEqualByComparingTo("7500.00");
        assertThat(response.netAmount()).isEqualByComparingTo("-7500.00");
    }

    private LedgerEntry entry(LedgerEntryType type, String amount, LedgerDirection direction) {
        LedgerEntry entry = new LedgerEntry();
        entry.setEntryType(type);
        entry.setAmount(new BigDecimal(amount));
        entry.setDirection(direction);
        return entry;
    }
}
