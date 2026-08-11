package com.khanh.fooddelivery.catalog_service.service;

import com.khanh.fooddelivery.catalog_service.dto.request.OptionValueCreateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.OptionValueUpdateRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.OptionValueResponse;
import java.util.List;
import java.util.UUID;

public interface OptionValueService {
    OptionValueResponse create(UUID itemId, UUID optionGroupId, OptionValueCreateRequest request);

    OptionValueResponse get(UUID itemId, UUID optionGroupId, UUID optionValueId);

    List<OptionValueResponse> list(UUID itemId, UUID optionGroupId);

    OptionValueResponse update(
            UUID itemId, UUID optionGroupId, UUID optionValueId, OptionValueUpdateRequest request);

    OptionValueResponse markAvailable(UUID itemId, UUID optionGroupId, UUID optionValueId);

    OptionValueResponse markUnavailable(UUID itemId, UUID optionGroupId, UUID optionValueId);
}
