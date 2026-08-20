package com.khanh.fooddelivery.catalog_service.repository;

import com.khanh.fooddelivery.catalog_service.entity.OptionTemplateValue;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OptionTemplateValueRepository extends JpaRepository<OptionTemplateValue, UUID> {
    List<OptionTemplateValue> findAllByTemplateIdInOrderBySortOrderAsc(List<UUID> templateIds);

    void deleteByTemplateId(UUID templateId);
}
