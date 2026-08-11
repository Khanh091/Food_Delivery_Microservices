package com.khanh.fooddelivery.catalog_service.repository;

import com.khanh.fooddelivery.catalog_service.entity.CatalogItem;
import com.khanh.fooddelivery.catalog_service.enums.CatalogStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogItemRepository extends JpaRepository<CatalogItem, UUID> {
    List<CatalogItem> findAllByRestaurantIdOrderByCreatedAtAsc(UUID restaurantId);

    List<CatalogItem> findAllByIdInAndStatus(List<UUID> itemIds, CatalogStatus status);

    java.util.Optional<CatalogItem> findByIdAndRestaurantIdAndStatus(
            UUID id, UUID restaurantId, CatalogStatus status);
}
