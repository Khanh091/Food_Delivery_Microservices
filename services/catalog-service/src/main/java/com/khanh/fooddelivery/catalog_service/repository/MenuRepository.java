package com.khanh.fooddelivery.catalog_service.repository;

import com.khanh.fooddelivery.catalog_service.entity.Menu;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<Menu, UUID> {
    List<Menu> findAllByRestaurantId(UUID restaurantId);

    List<Menu> findAllByBranchId(UUID branchId);
}
