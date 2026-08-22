package com.khanh.fooddelivery.order_service.placement;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderPlacementRequestRepository extends JpaRepository<OrderPlacementRequest, UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO order_placement_requests (
                id, customer_id, idempotency_key_hash, request_fingerprint,
                reserved_order_id, branch_id, cart_version, status,
                claim_token, processing_until, created_at, updated_at
            ) VALUES (
                :id, :customerId, :keyHash, :fingerprint,
                :reservedOrderId, :branchId, :cartVersion, 'PROCESSING',
                :claimToken, :processingUntil, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            )
            ON CONFLICT (customer_id, idempotency_key_hash) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("id") UUID id,
            @Param("customerId") UUID customerId,
            @Param("keyHash") String keyHash,
            @Param("fingerprint") String fingerprint,
            @Param("reservedOrderId") UUID reservedOrderId,
            @Param("branchId") UUID branchId,
            @Param("cartVersion") long cartVersion,
            @Param("claimToken") UUID claimToken,
            @Param("processingUntil") Instant processingUntil
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OrderPlacementRequest> findByCustomerIdAndIdempotencyKeyHash(
            UUID customerId,
            String idempotencyKeyHash
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE order_placement_requests
            SET status = 'COMPLETED', claim_token = NULL,
                processing_until = NULL, updated_at = CURRENT_TIMESTAMP
            WHERE id = :id AND reserved_order_id = :reservedOrderId
              AND status = 'PROCESSING'
            """, nativeQuery = true)
    int recoverCompleted(
            @Param("id") UUID id,
            @Param("reservedOrderId") UUID reservedOrderId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE order_placement_requests
            SET status = 'COMPLETED', claim_token = NULL,
                processing_until = NULL, updated_at = CURRENT_TIMESTAMP
            WHERE id = :id AND claim_token = :claimToken
              AND status = 'PROCESSING'
            """, nativeQuery = true)
    int markCompleted(
            @Param("id") UUID id,
            @Param("claimToken") UUID claimToken
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE order_placement_requests
            SET processing_until = :releasedAt, updated_at = CURRENT_TIMESTAMP
            WHERE id = :id AND claim_token = :claimToken
              AND status = 'PROCESSING'
            """, nativeQuery = true)
    int release(
            @Param("id") UUID id,
            @Param("claimToken") UUID claimToken,
            @Param("releasedAt") Instant releasedAt
    );
}
