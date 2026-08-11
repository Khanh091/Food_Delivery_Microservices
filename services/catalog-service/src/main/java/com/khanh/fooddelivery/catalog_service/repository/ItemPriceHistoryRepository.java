package com.khanh.fooddelivery.catalog_service.repository;

import com.khanh.fooddelivery.catalog_service.entity.ItemPriceHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemPriceHistoryRepository extends JpaRepository<ItemPriceHistory, UUID> {
    List<ItemPriceHistory> findAllByBranchItemId(UUID branchItemId);
}
