package com.khanh.fooddelivery.restaurant_service.entity;

import com.khanh.fooddelivery.restaurant_service.common.entity.BaseAuditEntity;
import com.khanh.fooddelivery.restaurant_service.enums.SystemRoleSyncOperation;
import com.khanh.fooddelivery.restaurant_service.enums.SystemRoleSyncStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "system_role_sync_requests")
@Getter
@Setter
@NoArgsConstructor
public class SystemRoleSyncRequest extends BaseAuditEntity {
    @Id private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation", nullable = false)
    private SystemRoleSyncOperation operation;

    @Column(name = "system_role", nullable = false, length = 50)
    private String systemRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SystemRoleSyncStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Version private long version;

    @PrePersist
    void id() {
        if (id == null) id = UUID.randomUUID();
    }
}