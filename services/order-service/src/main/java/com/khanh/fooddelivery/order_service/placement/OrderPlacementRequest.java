package com.khanh.fooddelivery.order_service.placement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
        name = "order_placement_requests",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_order_placement_customer_key",
                columnNames = {"customer_id", "idempotency_key_hash"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class OrderPlacementRequest {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID customerId;

    @Column(nullable = false, length = 64)
    private String idempotencyKeyHash;

    @Column(nullable = false, length = 64)
    private String requestFingerprint;

    @Column(nullable = false, unique = true)
    private UUID reservedOrderId;

    @Column(nullable = false)
    private UUID branchId;

    @Column(nullable = false)
    private long cartVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OrderPlacementStatus status;

    private UUID claimToken;

    private Instant processingUntil;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
