package com.khanh.fooddelivery.catalog_service.service.impl;

import com.khanh.fooddelivery.catalog_service.dto.request.OptionValueCreateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.OptionValueUpdateRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.OptionValueResponse;
import com.khanh.fooddelivery.catalog_service.entity.CatalogItem;
import com.khanh.fooddelivery.catalog_service.entity.OptionGroup;
import com.khanh.fooddelivery.catalog_service.entity.OptionValue;
import com.khanh.fooddelivery.catalog_service.exception.AppException;
import com.khanh.fooddelivery.catalog_service.exception.ErrorCode;
import com.khanh.fooddelivery.catalog_service.mapper.OptionValueMapper;
import com.khanh.fooddelivery.catalog_service.outbox.CatalogEventData;
import com.khanh.fooddelivery.catalog_service.outbox.CatalogEventType;
import com.khanh.fooddelivery.catalog_service.outbox.OutboxEventService;
import com.khanh.fooddelivery.catalog_service.repository.OptionGroupRepository;
import com.khanh.fooddelivery.catalog_service.repository.OptionValueRepository;
import com.khanh.fooddelivery.catalog_service.service.CatalogAuthorizationService;
import com.khanh.fooddelivery.catalog_service.service.OptionValueService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class OptionValueServiceImpl implements OptionValueService {
    private final OptionGroupRepository groupRepository;
    private final OptionValueRepository valueRepository;
    private final OptionValueMapper valueMapper;
    private final CatalogAuthorizationService authorizationService;
    private final OutboxEventService outboxEventService;

    @Override
    public OptionValueResponse create(UUID itemId, UUID groupId, OptionValueCreateRequest request) {
        OptionGroup group = requiredGroup(itemId, groupId);
        authorize(group.getItem());
        OptionValue value = valueMapper.toEntity(request);
        value.setOptionGroup(group);
        value.setIsAvailable(true);
        if (value.getSortOrder() == null) value.setSortOrder(0);
        OptionValue savedValue = valueRepository.save(value);
        enqueue(savedValue, "CREATED");
        return valueMapper.toResponse(savedValue);
    }

    @Override
    @Transactional(readOnly = true)
    public OptionValueResponse get(UUID itemId, UUID groupId, UUID valueId) {
        OptionValue value = requiredValue(itemId, groupId, valueId);
        authorize(value.getOptionGroup().getItem());
        return valueMapper.toResponse(value);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OptionValueResponse> list(UUID itemId, UUID groupId) {
        OptionGroup group = requiredGroup(itemId, groupId);
        authorize(group.getItem());
        return valueMapper.toResponses(
                valueRepository.findAllByOptionGroupIdOrderBySortOrderAsc(groupId));
    }

    @Override
    public OptionValueResponse update(
            UUID itemId, UUID groupId, UUID valueId, OptionValueUpdateRequest request) {
        OptionValue value = requiredValue(itemId, groupId, valueId);
        authorize(value.getOptionGroup().getItem());
        valueMapper.update(request, value);
        OptionValue savedValue = valueRepository.save(value);
        enqueue(savedValue, "UPDATED");
        return valueMapper.toResponse(savedValue);
    }

    @Override
    public OptionValueResponse markAvailable(UUID itemId, UUID groupId, UUID valueId) {
        return changeAvailability(itemId, groupId, valueId, true);
    }

    @Override
    public OptionValueResponse markUnavailable(UUID itemId, UUID groupId, UUID valueId) {
        return changeAvailability(itemId, groupId, valueId, false);
    }

    private OptionValueResponse changeAvailability(
            UUID itemId, UUID groupId, UUID valueId, boolean available) {
        OptionValue value = requiredValue(itemId, groupId, valueId);
        authorize(value.getOptionGroup().getItem());
        if (Boolean.valueOf(available).equals(value.getIsAvailable())) {
            return valueMapper.toResponse(value);
        }
        value.setIsAvailable(available);
        OptionValue savedValue = valueRepository.save(value);
        enqueue(savedValue, available ? "AVAILABLE" : "UNAVAILABLE");
        return valueMapper.toResponse(savedValue);
    }

    private OptionGroup requiredGroup(UUID itemId, UUID groupId) {
        return groupRepository
                .findByIdAndItemId(groupId, itemId)
                .orElseThrow(() -> new AppException(ErrorCode.OPTION_GROUP_NOT_FOUND));
    }

    private OptionValue requiredValue(UUID itemId, UUID groupId, UUID valueId) {
        requiredGroup(itemId, groupId);
        return valueRepository
                .findByIdAndOptionGroupId(valueId, groupId)
                .orElseThrow(() -> new AppException(ErrorCode.OPTION_VALUE_NOT_FOUND));
    }

    private void authorize(CatalogItem item) {
        authorizationService.requireRestaurantCatalogAccess(item.getRestaurantId());
    }

    private void enqueue(OptionValue value, String action) {
        outboxEventService.enqueue(
                CatalogEventType.OPTION_VALUE_CHANGED,
                "OPTION_VALUE",
                value.getId(),
                CatalogEventData.optionValue(value, action));
    }
}
