package com.khanh.fooddelivery.catalog_service.service;

import com.khanh.fooddelivery.catalog_service.dto.request.OptionGroupCreateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.OptionGroupUpdateRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.OptionGroupResponse;
import java.util.List;
import java.util.UUID;

public interface OptionGroupService {
    OptionGroupResponse create(UUID itemId, OptionGroupCreateRequest request);

    OptionGroupResponse get(UUID itemId, UUID optionGroupId);

    List<OptionGroupResponse> list(UUID itemId);

    OptionGroupResponse update(UUID itemId, UUID optionGroupId, OptionGroupUpdateRequest request);

    OptionGroupResponse activate(UUID itemId, UUID optionGroupId);

    OptionGroupResponse deactivate(UUID itemId, UUID optionGroupId);
}
