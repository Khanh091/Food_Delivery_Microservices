package com.khanh.fooddelivery.catalog_service.service.impl;

import com.khanh.fooddelivery.catalog_service.dto.request.OptionGroupCreateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.OptionGroupUpdateRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.OptionGroupResponse;
import com.khanh.fooddelivery.catalog_service.entity.CatalogItem;
import com.khanh.fooddelivery.catalog_service.entity.OptionGroup;
import com.khanh.fooddelivery.catalog_service.enums.CatalogStatus;
import com.khanh.fooddelivery.catalog_service.exception.AppException;
import com.khanh.fooddelivery.catalog_service.exception.ErrorCode;
import com.khanh.fooddelivery.catalog_service.mapper.OptionGroupMapper;
import com.khanh.fooddelivery.catalog_service.outbox.CatalogEventData;
import com.khanh.fooddelivery.catalog_service.outbox.CatalogEventType;
import com.khanh.fooddelivery.catalog_service.outbox.OutboxEventService;
import com.khanh.fooddelivery.catalog_service.repository.CatalogItemRepository;
import com.khanh.fooddelivery.catalog_service.repository.OptionGroupRepository;
import com.khanh.fooddelivery.catalog_service.service.CatalogAuthorizationService;
import com.khanh.fooddelivery.catalog_service.service.OptionGroupService;
import com.khanh.fooddelivery.catalog_service.validation.OptionSelectionRules;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class OptionGroupServiceImpl implements OptionGroupService {
    private final CatalogItemRepository itemRepository;
    private final OptionGroupRepository groupRepository;
    private final OptionGroupMapper groupMapper;
    private final CatalogAuthorizationService authorizationService;
    private final OutboxEventService outboxEventService;

    @Override
    public OptionGroupResponse create(UUID itemId, OptionGroupCreateRequest request) {
        CatalogItem item = requiredItem(itemId);
        authorize(item);
        OptionGroup group = groupMapper.toEntity(request);
        group.setItem(item);
        group.setStatus(CatalogStatus.ACTIVE);
        if (group.getSortOrder() == null) group.setSortOrder(0);
        validateSelection(group);
        OptionGroup savedGroup = groupRepository.save(group);
        enqueue(savedGroup, "CREATED");
        return groupMapper.toResponse(savedGroup);
    }

    @Override
    @Transactional(readOnly = true)
    public OptionGroupResponse get(UUID itemId, UUID groupId) {
        OptionGroup group = requiredGroup(itemId, groupId);
        authorize(group.getItem());
        return groupMapper.toResponse(group);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OptionGroupResponse> list(UUID itemId) {
        CatalogItem item = requiredItem(itemId);
        authorize(item);
        return groupMapper.toResponses(
                groupRepository.findAllByItemIdAndStatusOrderBySortOrderAsc(itemId, CatalogStatus.ACTIVE));
    }

    @Override
    public OptionGroupResponse update(UUID itemId, UUID groupId, OptionGroupUpdateRequest request) {
        OptionGroup group = requiredGroup(itemId, groupId);
        authorize(group.getItem());
        groupMapper.update(request, group);
        validateSelection(group);
        OptionGroup savedGroup = groupRepository.save(group);
        enqueue(savedGroup, "UPDATED");
        return groupMapper.toResponse(savedGroup);
    }

    @Override
    public OptionGroupResponse activate(UUID itemId, UUID groupId) {
        return changeStatus(itemId, groupId, CatalogStatus.ACTIVE);
    }

    @Override
    public OptionGroupResponse deactivate(UUID itemId, UUID groupId) {
        return changeStatus(itemId, groupId, CatalogStatus.INACTIVE);
    }

    private OptionGroupResponse changeStatus(UUID itemId, UUID groupId, CatalogStatus status) {
        OptionGroup group = requiredGroup(itemId, groupId);
        authorize(group.getItem());
        if (group.getStatus() == status) {
            return groupMapper.toResponse(group);
        }
        group.setStatus(status);
        OptionGroup savedGroup = groupRepository.save(group);
        enqueue(savedGroup, status.name());
        return groupMapper.toResponse(savedGroup);
    }

    private CatalogItem requiredItem(UUID itemId) {
        return itemRepository
                .findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.CATALOG_ITEM_NOT_FOUND));
    }

    private OptionGroup requiredGroup(UUID itemId, UUID groupId) {
        return groupRepository
                .findByIdAndItemId(groupId, itemId)
                .orElseThrow(() -> new AppException(ErrorCode.OPTION_GROUP_NOT_FOUND));
    }

    private void authorize(CatalogItem item) {
        authorizationService.requireRestaurantCatalogAccess(item.getRestaurantId());
    }

    private void validateSelection(OptionGroup group) {
        OptionSelectionRules.Normalized selection = OptionSelectionRules.normalize(
                group.getSelectionType(), group.getMinimumSelections(), group.getMaximumSelections());
        group.setMinimumSelections(selection.minimumSelections());
        group.setMaximumSelections(selection.maximumSelections());
        group.setRequired(selection.required());
    }

    private void enqueue(OptionGroup group, String action) {
        outboxEventService.enqueue(
                CatalogEventType.OPTION_GROUP_CHANGED,
                "OPTION_GROUP",
                group.getId(),
                CatalogEventData.optionGroup(group, action));
    }
}
