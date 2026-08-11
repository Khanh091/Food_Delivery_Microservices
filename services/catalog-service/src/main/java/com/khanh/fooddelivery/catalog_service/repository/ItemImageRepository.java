package com.khanh.fooddelivery.catalog_service.repository;

import com.khanh.fooddelivery.catalog_service.entity.ItemImage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemImageRepository extends JpaRepository<ItemImage, UUID> {
    List<ItemImage> findAllByItemId(UUID itemId);
}
