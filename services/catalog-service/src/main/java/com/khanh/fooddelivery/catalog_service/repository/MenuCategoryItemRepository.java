package com.khanh.fooddelivery.catalog_service.repository;

import com.khanh.fooddelivery.catalog_service.entity.MenuCategoryItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuCategoryItemRepository extends JpaRepository<MenuCategoryItem, UUID> {
    List<MenuCategoryItem> findAllByCategoryId(UUID categoryId);

    List<MenuCategoryItem> findAllByItemId(UUID itemId);
}
