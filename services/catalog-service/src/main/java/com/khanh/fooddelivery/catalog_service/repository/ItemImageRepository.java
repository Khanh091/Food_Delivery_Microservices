package com.khanh.fooddelivery.catalog_service.repository;

import com.khanh.fooddelivery.catalog_service.entity.ItemImage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemImageRepository extends JpaRepository<ItemImage, UUID> {
    List<ItemImage> findAllByItemIdOrderByIsPrimaryDescSortOrderAscCreatedAtAsc(UUID itemId);

    Optional<ItemImage> findByIdAndItemId(UUID id, UUID itemId);

    Optional<ItemImage> findFirstByItemIdAndIsPrimaryTrue(UUID itemId);

    Optional<ItemImage> findFirstByItemIdOrderBySortOrderAscCreatedAtAsc(UUID itemId);

    boolean existsByItemId(UUID itemId);

    @Modifying
    @Query(
            "UPDATE ItemImage image SET image.isPrimary = false WHERE image.item.id = :itemId AND"
                    + " image.isPrimary = true")
    int clearPrimaryByItemId(@Param("itemId") UUID itemId);
}
