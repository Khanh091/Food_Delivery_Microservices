package com.khanh.fooddelivery.catalog_service.repository;

import com.khanh.fooddelivery.catalog_service.entity.CatalogItem;
import com.khanh.fooddelivery.catalog_service.enums.CatalogStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CatalogItemRepository extends JpaRepository<CatalogItem, UUID> {
    List<CatalogItem> findAllByRestaurantIdOrderByCreatedAtAsc(UUID restaurantId);

    List<CatalogItem> findAllByIdInAndStatus(List<UUID> itemIds, CatalogStatus status);

    java.util.Optional<CatalogItem> findByIdAndRestaurantIdAndStatus(
            UUID id, UUID restaurantId, CatalogStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from CatalogItem item where item.id = :itemId")
    java.util.Optional<CatalogItem> findByIdForUpdate(@Param("itemId") UUID itemId);

    @Query("select item.id from CatalogItem item order by item.id")
    List<UUID> findSnapshotIds(Pageable pageable);

    @Query("select item.id from CatalogItem item where item.id > :afterId order by item.id")
    List<UUID> findSnapshotIdsAfter(@Param("afterId") UUID afterId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from CatalogItem item where item.id in :itemIds order by item.id")
    List<CatalogItem> findAllByIdInForUpdate(@Param("itemIds") List<UUID> itemIds);
}
