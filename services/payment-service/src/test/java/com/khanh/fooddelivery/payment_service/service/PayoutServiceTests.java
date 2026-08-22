package com.khanh.fooddelivery.payment_service.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.khanh.fooddelivery.payment_service.entity.Settlement;
import com.khanh.fooddelivery.payment_service.entity.Payout;
import com.khanh.fooddelivery.payment_service.model.PayoutStatus;
import com.khanh.fooddelivery.payment_service.model.SettlementBeneficiaryType;
import com.khanh.fooddelivery.payment_service.model.SettlementStatus;
import com.khanh.fooddelivery.payment_service.mapper.PayoutMapper;
import com.khanh.fooddelivery.payment_service.provider.PayoutGatewayResolver;
import com.khanh.fooddelivery.payment_service.repository.PayoutRepository;
import com.khanh.fooddelivery.payment_service.repository.SettlementRepository;
import com.khanh.fooddelivery.payment_service.service.impl.PayoutServiceImpl;
import com.khanh.fooddelivery.payment_service.service.impl.PayoutTransactionServiceImpl;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PayoutServiceTests {
    private final SettlementRepository settlements = Mockito.mock(SettlementRepository.class);
    private final PayoutRepository payouts = Mockito.mock(PayoutRepository.class);
    private final LedgerService ledger = Mockito.mock(LedgerService.class);

    @Test
    void negativeCodDebtCannotCreatePayout() {
        UUID id = UUID.randomUUID();
        Settlement settlement = new Settlement();
        settlement.setId(id);
        settlement.setStatus(SettlementStatus.READY);
        settlement.setBeneficiaryType(SettlementBeneficiaryType.RESTAURANT);
        settlement.setBeneficiaryId(UUID.randomUUID());
        settlement.setNetAmount(new BigDecimal("-15000.00"));
        when(settlements.findWithLockById(id)).thenReturn(Optional.of(settlement));
        when(payouts.findBySettlementIdForUpdate(id)).thenReturn(Optional.empty());
        PayoutTransactionServiceImpl service = new PayoutTransactionServiceImpl(settlements, payouts, ledger,
                java.time.Clock.systemUTC());

        assertThatThrownBy(() -> service.prepare(id)).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no payable amount");
        verify(payouts, never()).save(Mockito.any());
    }

    @Test
    void payoutCompletionRequiresProviderConfirmation() {
        UUID settlementId = UUID.randomUUID();
        UUID payoutId = UUID.randomUUID();
        Settlement settlement = new Settlement();
        settlement.setId(settlementId);
        settlement.setStatus(SettlementStatus.PROCESSING);
        Payout payout = new Payout();
        payout.setId(payoutId);
        payout.setSettlementId(settlementId);
        payout.setStatus(PayoutStatus.PENDING);
        when(payouts.findWithLockById(payoutId)).thenReturn(Optional.of(payout));
        when(settlements.findWithLockById(settlementId)).thenReturn(Optional.of(settlement));
        PayoutTransactionServiceImpl service = new PayoutTransactionServiceImpl(settlements, payouts, ledger,
                java.time.Clock.systemUTC());

        assertThatThrownBy(() -> service.complete(payoutId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("provider confirmation");
        verify(ledger, never()).record(Mockito.any());
    }

    @Test
    void persistedProviderReferenceIsFinalizedWithoutSubmittingAgain() {
        UUID settlementId = UUID.randomUUID();
        Payout payout = new Payout();
        payout.setId(UUID.randomUUID());
        payout.setSettlementId(settlementId);
        payout.setStatus(PayoutStatus.PROCESSING);
        payout.setProviderReference("provider-transfer-1");

        PayoutTransactionService transactions = Mockito.mock(PayoutTransactionService.class);
        PayoutGatewayResolver gateways = Mockito.mock(PayoutGatewayResolver.class);
        PayoutMapper mapper = Mockito.mock(PayoutMapper.class);
        when(transactions.prepare(settlementId)).thenReturn(payout);
        when(transactions.complete(payout.getId())).thenReturn(payout);

        PayoutServiceImpl service = new PayoutServiceImpl(transactions, gateways, mapper);

        service.payout(settlementId);

        verify(gateways, never()).resolve(Mockito.any());
        verify(transactions).complete(payout.getId());
        verify(transactions, never()).markFailed(Mockito.any(), Mockito.any());
    }
}
