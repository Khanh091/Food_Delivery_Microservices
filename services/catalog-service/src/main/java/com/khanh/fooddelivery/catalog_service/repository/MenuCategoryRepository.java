package com.khanh.fooddelivery.catalog_service.repository;

import com.khanh.fooddelivery.catalog_service.entity.MenuCategory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, UUID> {
    List<MenuCategory> findAllByMenuIdOrderBySortOrderAsc(UUID menuId);

    java.util.Optional<MenuCategory> findByIdAndMenuId(UUID id, UUID menuId);
}
