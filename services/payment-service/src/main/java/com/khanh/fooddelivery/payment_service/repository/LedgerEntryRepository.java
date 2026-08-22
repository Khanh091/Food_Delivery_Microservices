package com.khanh.fooddelivery.payment_service.repository;

import com.khanh.fooddelivery.payment_service.entity.LedgerEntry;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
    Optional<LedgerEntry> findByIdempotencyReference(String reference);
    List<LedgerEntry> findByOccurredAtGreaterThanEqualAndOccurredAtLessThan(Instant from, Instant to);
    List<LedgerEntry> findByOwnerTypeAndOwnerIdAndOccurredAtBetween(String ownerType, UUID ownerId,
                                                                     Instant from, Instant to);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select entry from LedgerEntry entry
            where entry.ownerType = :ownerType
              and entry.ownerId = :ownerId
              and entry.occurredAt >= :periodFrom
              and entry.occurredAt < :periodTo
              and entry.settlementId is null
            order by entry.occurredAt asc
            """)
    List<LedgerEntry> findUnsettledByOwnerAndPeriod(@Param("ownerType") String ownerType,
                                                     @Param("ownerId") UUID ownerId,
                                                     @Param("periodFrom") Instant periodFrom,
                                                     @Param("periodTo") Instant periodTo);
}
