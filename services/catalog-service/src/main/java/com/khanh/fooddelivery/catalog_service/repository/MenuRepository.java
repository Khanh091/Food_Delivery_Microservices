package com.khanh.fooddelivery.catalog_service.repository;

import com.khanh.fooddelivery.catalog_service.entity.Menu;
import com.khanh.fooddelivery.catalog_service.enums.CatalogStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<Menu, UUID> {
    List<Menu> findAllByRestaurantId(UUID restaurantId);

    List<Menu> findAllByBranchId(UUID branchId);

    List<Menu> findAllByRestaurantIdAndBranchIdOrderByCreatedAtAsc(
            UUID restaurantId, UUID branchId);

    List<Menu> findAllByRestaurantIdAndBranchIdAndStatusOrderByCreatedAtAsc(
            UUID restaurantId, UUID branchId, CatalogStatus status);
}
