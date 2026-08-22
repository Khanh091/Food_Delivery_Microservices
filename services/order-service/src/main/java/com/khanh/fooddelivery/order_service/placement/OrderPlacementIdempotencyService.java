package com.khanh.fooddelivery.order_service.placement;

import com.khanh.fooddelivery.order_service.dto.request.CreateOrderRequest;
import com.khanh.fooddelivery.order_service.exception.AppException;
import com.khanh.fooddelivery.order_service.exception.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderPlacementIdempotencyService {

    private static final int MAX_KEY_LENGTH = 200;

    private final OrderPlacementRequestRepository requests;
    private final OrderPlacementFingerprint fingerprints;
    private final Clock clock;

    @Value("${app.order-placement.processing-lease:2m}")
    private Duration processingLease;

    @Transactional
    public OrderPlacementClaim claim(
            UUID customerId,
            String rawKey,
            CreateOrderRequest request
    ) {
        if (customerId == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        String key = normalizeKey(rawKey);
        String keyHash = fingerprints.keyHash(key);
        String requestFingerprint = fingerprints.of(request);
        UUID requestId = UUID.randomUUID();
        UUID reservedOrderId = UUID.randomUUID();
        UUID claimToken = UUID.randomUUID();
        Instant now = clock.instant();
        int inserted = requests.insertIfAbsent(
                requestId,
                customerId,
                keyHash,
                requestFingerprint,
                reservedOrderId,
                request.branchId(),
                request.cartVersion(),
                claimToken,
                now.plus(lease())
        );

        OrderPlacementRequest placement = requests
                .findByCustomerIdAndIdempotencyKeyHash(customerId, keyHash)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_IDEMPOTENCY_RECOVERY_FAILED));
        if (!placement.getRequestFingerprint().equals(requestFingerprint)) {
            throw new AppException(ErrorCode.ORDER_IDEMPOTENCY_CONFLICT);
        }
        if (placement.getStatus() == OrderPlacementStatus.COMPLETED) {
            return new OrderPlacementClaim(
                    placement.getId(), placement.getReservedOrderId(), placement.getBranchId(),
                    placement.getCartVersion(), null, OrderPlacementClaim.Status.COMPLETED);
        }
        if (inserted == 1) {
            return new OrderPlacementClaim(
                    placement.getId(), placement.getReservedOrderId(), placement.getBranchId(),
                    placement.getCartVersion(), claimToken, OrderPlacementClaim.Status.ACTIVE);
        }
        if (placement.getProcessingUntil() != null && placement.getProcessingUntil().isAfter(now)) {
            return new OrderPlacementClaim(
                    placement.getId(), placement.getReservedOrderId(), placement.getBranchId(),
                    placement.getCartVersion(), null, OrderPlacementClaim.Status.IN_PROGRESS);
        }

        placement.setClaimToken(claimToken);
        placement.setProcessingUntil(now.plus(lease()));
        placement.setStatus(OrderPlacementStatus.PROCESSING);
        requests.saveAndFlush(placement);
        return new OrderPlacementClaim(
                placement.getId(), placement.getReservedOrderId(), placement.getBranchId(),
                placement.getCartVersion(), claimToken, OrderPlacementClaim.Status.ACTIVE);
    }

    @Transactional
    public boolean markCompleted(UUID requestId, UUID claimToken) {
        return requests.markCompleted(requestId, claimToken) == 1;
    }

    @Transactional
    public boolean recoverCompleted(UUID requestId, UUID reservedOrderId) {
        return requests.recoverCompleted(requestId, reservedOrderId) == 1;
    }

    @Transactional
    public boolean release(UUID requestId, UUID claimToken) {
        return requests.release(requestId, claimToken, clock.instant()) == 1;
    }

    private String normalizeKey(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            throw new AppException(ErrorCode.ORDER_IDEMPOTENCY_KEY_REQUIRED);
        }
        String key = rawKey.trim();
        if (key.length() > MAX_KEY_LENGTH || key.chars().anyMatch(Character::isISOControl)) {
            throw new AppException(ErrorCode.ORDER_IDEMPOTENCY_KEY_INVALID);
        }
        return key;
    }

    private Duration lease() {
        return processingLease == null || processingLease.isNegative() || processingLease.isZero()
                ? Duration.ofMinutes(2)
                : processingLease;
    }
}
