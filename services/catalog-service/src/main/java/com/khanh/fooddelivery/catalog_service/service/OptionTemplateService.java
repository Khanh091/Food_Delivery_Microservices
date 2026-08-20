package com.khanh.fooddelivery.catalog_service.service;

import com.khanh.fooddelivery.catalog_service.dto.request.OptionTemplateUpsertRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.OptionTemplateBatchCopyRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.OptionGroupResponse;
import com.khanh.fooddelivery.catalog_service.dto.response.OptionTemplatePageResponse;
import com.khanh.fooddelivery.catalog_service.dto.response.OptionTemplateResponse;
import java.util.UUID;
import java.util.List;

public interface OptionTemplateService {
    OptionTemplatePageResponse list(UUID restaurantId, String query, int page, int size);

    OptionTemplateResponse get(UUID restaurantId, UUID templateId);

    OptionTemplateResponse create(UUID restaurantId, OptionTemplateUpsertRequest request);

    OptionTemplateResponse update(UUID restaurantId, UUID templateId, OptionTemplateUpsertRequest request);

    OptionTemplateResponse activate(UUID restaurantId, UUID templateId);

    OptionTemplateResponse deactivate(UUID restaurantId, UUID templateId);

    OptionGroupResponse copyToItem(UUID restaurantId, UUID templateId, UUID itemId);

    List<OptionGroupResponse> copyToItemBatch(
            UUID restaurantId, UUID itemId, OptionTemplateBatchCopyRequest request);
}
