package com.khanh.fooddelivery.catalog_service.repository;

import com.khanh.fooddelivery.catalog_service.entity.OptionGroup;
import com.khanh.fooddelivery.catalog_service.enums.CatalogStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OptionGroupRepository extends JpaRepository<OptionGroup, UUID> {
    List<OptionGroup> findAllByItemIdOrderBySortOrderAsc(UUID itemId);

    List<OptionGroup> findAllByItemIdAndStatusOrderBySortOrderAsc(UUID itemId, CatalogStatus status);

    Optional<OptionGroup> findByIdAndItemId(UUID id, UUID itemId);

    boolean existsByItemIdAndNameAndStatus(UUID itemId, String name, CatalogStatus status);

    List<OptionGroup> findAllByItemIdInAndStatusOrderBySortOrderAsc(
            List<UUID> itemIds, CatalogStatus status);
}
