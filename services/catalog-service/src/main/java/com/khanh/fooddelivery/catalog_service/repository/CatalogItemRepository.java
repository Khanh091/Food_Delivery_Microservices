package com.khanh.fooddelivery.catalog_service.repository;

import com.khanh.fooddelivery.catalog_service.entity.CatalogItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogItemRepository extends JpaRepository<CatalogItem, UUID> {
    List<CatalogItem> findAllByRestaurantIdOrderByCreatedAtAsc(UUID restaurantId);
}
