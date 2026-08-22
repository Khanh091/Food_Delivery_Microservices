package com.khanh.fooddelivery.payment_service.repository;

import com.khanh.fooddelivery.payment_service.entity.FeePolicy;
import com.khanh.fooddelivery.payment_service.model.FeePolicyStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface FeePolicyRepository extends JpaRepository<FeePolicy, UUID> {
    Optional<FeePolicy> findTopByStatusAndEffectiveFromLessThanEqualAndEffectiveToIsNullOrderByEffectiveFromDesc(
            FeePolicyStatus status, Instant effectiveFrom);

    @Query("""
            select policy from FeePolicy policy
            where policy.status = :status
              and policy.effectiveFrom <= :at
              and (policy.effectiveTo is null or policy.effectiveTo > :at)
            order by policy.effectiveFrom desc
            """)
    Optional<FeePolicy> findCurrent(@Param("status") FeePolicyStatus status, @Param("at") Instant at);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select policy from FeePolicy policy
            where policy.status = :status
              and policy.effectiveFrom <= :at
              and (policy.effectiveTo is null or policy.effectiveTo > :at)
            order by policy.effectiveFrom desc
            """)
    Optional<FeePolicy> findCurrentForUpdate(@Param("status") FeePolicyStatus status, @Param("at") Instant at);

    List<FeePolicy> findAllByOrderByPolicyVersionDesc();
}
