package com.khanh.fooddelivery.catalog_service.repository;

import com.khanh.fooddelivery.catalog_service.entity.BranchItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchItemRepository extends JpaRepository<BranchItem, UUID> {
    boolean existsByBranchIdAndItemId(UUID branchId, UUID itemId);

    List<BranchItem> findAllByBranchId(UUID branchId);

    List<BranchItem> findAllByItemId(UUID itemId);

    List<BranchItem> findAllByBranchIdAndItemIdIn(UUID branchId, List<UUID> itemIds);

    java.util.Optional<BranchItem> findByBranchIdAndItemId(UUID branchId, UUID itemId);
}
