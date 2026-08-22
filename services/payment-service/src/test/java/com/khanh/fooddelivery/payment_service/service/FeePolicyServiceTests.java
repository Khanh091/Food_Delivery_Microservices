package com.khanh.fooddelivery.payment_service.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.payment_service.entity.FeePolicy;
import com.khanh.fooddelivery.payment_service.exception.PaymentException;
import com.khanh.fooddelivery.payment_service.mapper.FeePolicyMapper;
import com.khanh.fooddelivery.payment_service.model.FeePolicyStatus;
import com.khanh.fooddelivery.payment_service.repository.FeePolicyRepository;
import com.khanh.fooddelivery.payment_service.service.impl.FeePolicyServiceImpl;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.Mockito;

class FeePolicyServiceTests {
    private final FeePolicyRepository repository = Mockito.mock(FeePolicyRepository.class);
    private final FeePolicyServiceImpl service = new FeePolicyServiceImpl(repository,
            Mappers.getMapper(FeePolicyMapper.class),
            Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));

    @Test
    void missingActivePolicyFailsWithoutCreatingAnImplicitThirtyPercentPolicy() {
        when(repository.findCurrent(FeePolicyStatus.ACTIVE, Instant.parse("2026-01-01T00:00:00Z")))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.currentPolicy(Instant.parse("2026-01-01T00:00:00Z")))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("No active fee policy");
        verify(repository, never()).save(any(FeePolicy.class));
    }

    @Test
    void policyVersionsCannotBeInsertedOutOfOrderAcrossActiveRanges() {
        FeePolicy current = new FeePolicy();
        current.setId(UUID.randomUUID());
        current.setPolicyVersion(2);
        current.setStatus(FeePolicyStatus.ACTIVE);
        current.setEffectiveFrom(Instant.parse("2026-01-10T00:00:00Z"));
        when(repository.findAllByOrderByPolicyVersionDesc()).thenReturn(List.of(current));

        assertThatThrownBy(() -> service.createPolicy(new com.khanh.fooddelivery.payment_service.dto.request.FeePolicyRequest(
                        java.math.BigDecimal.TEN, java.math.BigDecimal.TEN,
                        Instant.parse("2026-01-09T00:00:00Z"))))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("after the latest active policy");
        verify(repository, never()).save(any(FeePolicy.class));
    }
}
