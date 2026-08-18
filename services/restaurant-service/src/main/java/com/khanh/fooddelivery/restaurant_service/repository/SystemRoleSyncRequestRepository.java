package com.khanh.fooddelivery.restaurant_service.repository;

import com.khanh.fooddelivery.restaurant_service.entity.SystemRoleSyncRequest;
import com.khanh.fooddelivery.restaurant_service.enums.SystemRoleSyncStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemRoleSyncRequestRepository extends JpaRepository<SystemRoleSyncRequest, UUID> {
    List<SystemRoleSyncRequest> findByStatusAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
            SystemRoleSyncStatus status, Instant now, Pageable pageable);

    Optional<SystemRoleSyncRequest>
            findTopByUserIdAndSystemRoleAndOperationAndStatusOrderByCreatedAtDesc(
                    UUID userId,
                    String systemRole,
                    com.khanh.fooddelivery.restaurant_service.enums.SystemRoleSyncOperation operation,
                    SystemRoleSyncStatus status);
}