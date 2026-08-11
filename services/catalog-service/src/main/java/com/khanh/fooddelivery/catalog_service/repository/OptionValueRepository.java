package com.khanh.fooddelivery.catalog_service.repository;

import com.khanh.fooddelivery.catalog_service.entity.OptionValue;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OptionValueRepository extends JpaRepository<OptionValue, UUID> {
    List<OptionValue> findAllByOptionGroupId(UUID optionGroupId);
}
