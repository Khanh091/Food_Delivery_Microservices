package com.khanh.fooddelivery.payment_service.repository;

import com.khanh.fooddelivery.payment_service.entity.Settlement;
import java.util.UUID;
import java.time.Instant;
import com.khanh.fooddelivery.payment_service.model.SettlementBeneficiaryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface SettlementRepository extends JpaRepository<Settlement, UUID> {
    java.util.Optional<Settlement> findByBeneficiaryTypeAndBeneficiaryIdAndPeriodFromAndPeriodTo(
            SettlementBeneficiaryType beneficiaryType, UUID beneficiaryId, Instant periodFrom, Instant periodTo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select settlement from Settlement settlement where settlement.id = :id")
    java.util.Optional<Settlement> findWithLockById(@Param("id") UUID id);
}
