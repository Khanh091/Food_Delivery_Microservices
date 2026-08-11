package com.khanh.fooddelivery.catalog_service.repository;

import com.khanh.fooddelivery.catalog_service.entity.OptionGroup;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OptionGroupRepository extends JpaRepository<OptionGroup, UUID> {
    List<OptionGroup> findAllByItemId(UUID itemId);
}
