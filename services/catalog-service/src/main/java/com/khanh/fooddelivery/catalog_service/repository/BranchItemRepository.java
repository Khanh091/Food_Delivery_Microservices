package com.khanh.fooddelivery.catalog_service.repository;

import com.khanh.fooddelivery.catalog_service.entity.BranchItem;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BranchItemRepository extends JpaRepository<BranchItem, UUID> {
    boolean existsByBranchIdAndItemId(UUID branchId, UUID itemId);

    List<BranchItem> findAllByBranchId(UUID branchId);

    List<BranchItem> findAllByItemId(UUID itemId);

    List<BranchItem> findAllByBranchIdAndItemIdIn(UUID branchId, List<UUID> itemIds);

    java.util.Optional<BranchItem> findByBranchIdAndItemId(UUID branchId, UUID itemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select branchItem from BranchItem branchItem where branchItem.id = :branchItemId")
    java.util.Optional<BranchItem> findByIdForUpdate(@Param("branchItemId") UUID branchItemId);

    @Query("select branchItem.id from BranchItem branchItem order by branchItem.id")
    List<UUID> findSnapshotIds(Pageable pageable);

    @Query(
            "select branchItem.id from BranchItem branchItem "
                    + "where branchItem.id > :afterId order by branchItem.id")
    List<UUID> findSnapshotIdsAfter(@Param("afterId") UUID afterId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select branchItem from BranchItem branchItem where branchItem.id in :branchItemIds order by branchItem.id")
    List<BranchItem> findAllByIdInForUpdate(@Param("branchItemIds") List<UUID> branchItemIds);
}
