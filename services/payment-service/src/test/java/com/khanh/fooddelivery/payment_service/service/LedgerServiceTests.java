package com.khanh.fooddelivery.payment_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.payment_service.entity.LedgerEntry;
import com.khanh.fooddelivery.payment_service.model.LedgerDirection;
import com.khanh.fooddelivery.payment_service.model.LedgerEntryType;
import com.khanh.fooddelivery.payment_service.repository.LedgerEntryRepository;
import com.khanh.fooddelivery.payment_service.service.impl.LedgerServiceImpl;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class LedgerServiceTests {
    private final LedgerEntryRepository repository = Mockito.mock(LedgerEntryRepository.class);
    private final LedgerServiceImpl service = new LedgerServiceImpl(repository,
            Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));

    @Test
    void ownerIsTakenFromExplicitLedgerCommandNotEntryTypeName() {
        when(repository.findByIdempotencyReference("explicit-owner")).thenReturn(Optional.empty());
        when(repository.save(any(LedgerEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LedgerEntry entry = service.record(new LedgerCommand(
                "CUSTOMER", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                LedgerEntryType.RESTAURANT_PAYABLE_CREATED, LedgerDirection.CREDIT,
                new BigDecimal("10.00"), "VND", "explicit-owner"));

        assertThat(entry.getOwnerType()).isEqualTo("CUSTOMER");
    }

    @Test
    void settlementClaimCannotMoveAnAlreadyClaimedEntry() {
        LedgerEntry entry = new LedgerEntry();
        entry.setSettlementId(UUID.randomUUID());

        assertThatThrownBy(() -> service.claimForSettlement(java.util.List.of(entry), UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already claimed");
    }
}
