package com.khanh.fooddelivery.payment_service.service.impl;

import com.khanh.fooddelivery.payment_service.dto.request.FeePolicyRequest;
import com.khanh.fooddelivery.payment_service.dto.response.FeePolicyResponse;
import com.khanh.fooddelivery.payment_service.entity.FeePolicy;
import com.khanh.fooddelivery.payment_service.exception.PaymentException;
import com.khanh.fooddelivery.payment_service.mapper.FeePolicyMapper;
import com.khanh.fooddelivery.payment_service.model.FeePolicyStatus;
import com.khanh.fooddelivery.payment_service.repository.FeePolicyRepository;
import com.khanh.fooddelivery.payment_service.service.FeePolicyService;
import java.math.BigDecimal;
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
public class FeePolicyServiceImpl implements FeePolicyService {
    private final FeePolicyRepository repository;
    private final FeePolicyMapper mapper;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public FeePolicy currentPolicy(Instant at) {
        return repository.findCurrent(FeePolicyStatus.ACTIVE, at)
                .orElseThrow(() -> new PaymentException("PAYMENT_503", HttpStatus.SERVICE_UNAVAILABLE,
                        "No active fee policy is configured"));
    }

    @Override
    @Transactional(readOnly = true)
    public FeePolicyResponse activePolicy() {
        return mapper.toResponse(currentPolicy(Instant.now(clock)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeePolicyResponse> history() {
        return repository.findAllByOrderByPolicyVersionDesc().stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional
    public FeePolicyResponse createPolicy(FeePolicyRequest request) {
        Instant now = Instant.now(clock);
        validate(request, now);
        List<FeePolicy> activePolicies = repository.findAllByOrderByPolicyVersionDesc().stream()
                .filter(policy -> policy.getStatus() == FeePolicyStatus.ACTIVE)
                .toList();
        activePolicies.stream()
                .map(FeePolicy::getEffectiveFrom)
                .filter(java.util.Objects::nonNull)
                .max(Instant::compareTo)
                .filter(latest -> !request.effectiveFrom().isAfter(latest))
                .ifPresent(latest -> {
                    throw conflict("Fee policy effectiveFrom must be after the latest active policy");
                });
        FeePolicy current = repository.findCurrentForUpdate(FeePolicyStatus.ACTIVE, request.effectiveFrom()).orElse(null);
        if (current != null && !current.getEffectiveFrom().isBefore(request.effectiveFrom())) {
            throw conflict("Fee policy cannot be effective at or before the current policy");
        }
        if (current != null) {
            current.setEffectiveTo(request.effectiveFrom());
            current.setStatus(FeePolicyStatus.INACTIVE);
        }

        int nextVersion = repository.findAllByOrderByPolicyVersionDesc().stream()
                .map(FeePolicy::getPolicyVersion)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(0) + 1;
        FeePolicy policy = new FeePolicy();
        policy.setId(UUID.randomUUID());
        policy.setPolicyVersion(nextVersion);
        policy.setRestaurantCommissionRate(rate(request.restaurantCommissionRate()));
        policy.setDriverCommissionRate(rate(request.driverCommissionRate()));
        policy.setEffectiveFrom(request.effectiveFrom());
        policy.setStatus(FeePolicyStatus.ACTIVE);
        return mapper.toResponse(repository.save(policy));
    }

    private void validate(FeePolicyRequest request, Instant now) {
        if (request == null || request.effectiveFrom() == null
                || request.restaurantCommissionRate() == null || request.driverCommissionRate() == null) {
            throw invalid("Fee policy fields are required");
        }
        if (!request.effectiveFrom().isAfter(now)) {
            throw conflict("Fee policy effectiveFrom must be in the future");
        }
        if (request.restaurantCommissionRate().compareTo(BigDecimal.ZERO) < 0
                || request.restaurantCommissionRate().compareTo(BigDecimal.valueOf(100)) > 0
                || request.driverCommissionRate().compareTo(BigDecimal.ZERO) < 0
                || request.driverCommissionRate().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw invalid("Fee policy rates must be between 0 and 100 percent");
        }
    }

    private BigDecimal rate(BigDecimal value) {
        return value.setScale(4, java.math.RoundingMode.HALF_UP);
    }

    private PaymentException invalid(String message) {
        return new PaymentException("PAYMENT_400", HttpStatus.BAD_REQUEST, message);
    }

    private PaymentException conflict(String message) {
        return new PaymentException("PAYMENT_409", HttpStatus.CONFLICT, message);
    }
}
