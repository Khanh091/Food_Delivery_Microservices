package com.khanh.fooddelivery.payment_service.repository;

import com.khanh.fooddelivery.payment_service.entity.FinancialSnapshot;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialSnapshotRepository extends JpaRepository<FinancialSnapshot, UUID> {
    Optional<FinancialSnapshot> findByOrderId(UUID orderId);
}
