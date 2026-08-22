package com.khanh.fooddelivery.payment_service.repository;

import com.khanh.fooddelivery.payment_service.entity.Payout;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PayoutRepository extends JpaRepository<Payout, UUID> {
    Optional<Payout> findBySettlementId(UUID settlementId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payout p where p.id = :id")
    Optional<Payout> findWithLockById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payout p where p.settlementId = :settlementId")
    Optional<Payout> findBySettlementIdForUpdate(@Param("settlementId") UUID settlementId);
}
