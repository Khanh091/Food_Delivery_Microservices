package com.khanh.fooddelivery.catalog_service.repository;

import com.khanh.fooddelivery.catalog_service.entity.MenuCategoryItem;
import com.khanh.fooddelivery.catalog_service.repository.projection.ItemPlacementCount;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MenuCategoryItemRepository extends JpaRepository<MenuCategoryItem, UUID> {
    boolean existsByCategoryIdAndItemId(UUID categoryId, UUID itemId);

    Optional<MenuCategoryItem> findByCategoryIdAndItemId(UUID categoryId, UUID itemId);

    List<MenuCategoryItem> findAllByCategoryIdOrderBySortOrderAsc(UUID categoryId);

    List<MenuCategoryItem> findAllByCategoryIdInOrderBySortOrderAsc(List<UUID> categoryIds);

    @Query(
            "select mapping.item.id as itemId, count(mapping) as placementCount "
                    + "from MenuCategoryItem mapping "
                    + "where mapping.item.id in :itemIds group by mapping.item.id")
    List<ItemPlacementCount> countPlacementsByItemIdIn(@Param("itemIds") List<UUID> itemIds);
}
