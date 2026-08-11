package com.khanh.fooddelivery.catalog_service.repository;

import com.khanh.fooddelivery.catalog_service.entity.MenuCategoryItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuCategoryItemRepository extends JpaRepository<MenuCategoryItem, UUID> {
    boolean existsByCategoryIdAndItemId(UUID categoryId, UUID itemId);

    Optional<MenuCategoryItem> findByCategoryIdAndItemId(UUID categoryId, UUID itemId);

    List<MenuCategoryItem> findAllByCategoryIdOrderBySortOrderAsc(UUID categoryId);

    List<MenuCategoryItem> findAllByCategoryIdInOrderBySortOrderAsc(List<UUID> categoryIds);
}
