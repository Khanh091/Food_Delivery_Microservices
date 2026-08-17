package com.khanh.fooddelivery.catalog_service.service.impl;

import com.khanh.fooddelivery.catalog_service.dto.request.internal.CartItemValidationRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.internal.CartItemValidationResponse;
import com.khanh.fooddelivery.catalog_service.entity.BranchItem;
import com.khanh.fooddelivery.catalog_service.entity.CatalogItem;
import com.khanh.fooddelivery.catalog_service.entity.OptionGroup;
import com.khanh.fooddelivery.catalog_service.entity.OptionValue;
import com.khanh.fooddelivery.catalog_service.enums.CatalogStatus;
import com.khanh.fooddelivery.catalog_service.exception.AppException;
import com.khanh.fooddelivery.catalog_service.exception.ErrorCode;
import com.khanh.fooddelivery.catalog_service.repository.BranchItemRepository;
import com.khanh.fooddelivery.catalog_service.repository.CatalogItemRepository;
import com.khanh.fooddelivery.catalog_service.repository.ItemImageRepository;
import com.khanh.fooddelivery.catalog_service.repository.OptionGroupRepository;
import com.khanh.fooddelivery.catalog_service.repository.OptionValueRepository;
import com.khanh.fooddelivery.catalog_service.service.CartItemValidationService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartItemValidationServiceImpl implements CartItemValidationService {
    private final CatalogItemRepository items;
    private final BranchItemRepository branchItems;
    private final ItemImageRepository images;
    private final OptionGroupRepository optionGroups;
    private final OptionValueRepository optionValues;

    @Override
    public CartItemValidationResponse validate(CartItemValidationRequest request) {
        List<UUID> selectedIds = request.selectedOptionValueIds();
        if (selectedIds.stream().anyMatch(id -> id == null)
                || new HashSet<>(selectedIds).size() != selectedIds.size()) {
            throw new AppException(ErrorCode.INVALID_OPTION_SELECTION);
        }

        CatalogItem item =
                items.findByIdAndRestaurantIdAndStatus(
                                request.catalogItemId(), request.restaurantId(), CatalogStatus.ACTIVE)
                        .orElseThrow(() -> new AppException(ErrorCode.CATALOG_ITEM_NOT_FOUND));
        BranchItem branchItem =
                branchItems
                        .findByBranchIdAndItemId(request.branchId(), item.getId())
                        .orElseThrow(() -> new AppException(ErrorCode.BRANCH_ITEM_NOT_FOUND));
        if (!isEffectivelyAvailable(branchItem)) {
            throw new AppException(ErrorCode.ITEM_UNAVAILABLE);
        }

        List<OptionGroup> groups =
                optionGroups.findAllByItemIdInAndStatusOrderBySortOrderAsc(
                        List.of(item.getId()), CatalogStatus.ACTIVE);
        Map<UUID, OptionGroup> groupsById = new HashMap<>();
        for (OptionGroup group : groups) groupsById.put(group.getId(), group);

        Map<UUID, OptionValue> selectedValuesById = new HashMap<>();
        for (OptionValue value : optionValues.findAllById(selectedIds)) {
            selectedValuesById.put(value.getId(), value);
        }
        if (selectedValuesById.size() != selectedIds.size()) {
            throw new AppException(ErrorCode.INVALID_OPTION_SELECTION);
        }

        Map<UUID, List<OptionValue>> valuesByGroup = new HashMap<>();
        for (OptionValue value : selectedValuesById.values()) {
            OptionGroup group = value.getOptionGroup();
            if (!Boolean.TRUE.equals(value.getIsAvailable()) || !groupsById.containsKey(group.getId())) {
                throw new AppException(ErrorCode.INVALID_OPTION_SELECTION);
            }
            valuesByGroup.computeIfAbsent(group.getId(), ignored -> new java.util.ArrayList<>()).add(value);
        }
        validateSelections(groups, valuesByGroup);

        List<CartItemValidationResponse.SelectedOptionResponse> selectedOptions =
                groups.stream()
                        .flatMap(
                                group ->
                                        valuesByGroup.getOrDefault(group.getId(), List.of()).stream()
                                                .sorted(
                                                        Comparator.comparing(
                                                                OptionValue::getSortOrder,
                                                                Comparator.nullsLast(
                                                                        Integer::compareTo)))
                                                .map(
                                                        value ->
                                                                new CartItemValidationResponse
                                                                        .SelectedOptionResponse(
                                                                        group.getId(),
                                                                        value.getId(),
                                                                        group.getName(),
                                                                        value.getName(),
                                                                        value.getAdditionalPrice())))
                        .toList();
        BigDecimal optionUnitPrice =
                selectedOptions.stream()
                        .map(CartItemValidationResponse.SelectedOptionResponse::additionalPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        String primaryImageUrl =
                images.findFirstByItemIdAndIsPrimaryTrue(item.getId())
                        .map(image -> image.getImageUrl())
                        .orElse(null);
        return new CartItemValidationResponse(
                item.getId(),
                branchItem.getId(),
                item.getName(),
                primaryImageUrl,
                branchItem.getSellingPrice(),
                branchItem.getOriginalPrice(),
                item.getCurrency(),
                selectedOptions,
                optionUnitPrice,
                branchItem.getSellingPrice().add(optionUnitPrice));
    }

    private void validateSelections(
            Collection<OptionGroup> groups, Map<UUID, List<OptionValue>> valuesByGroup) {
        for (OptionGroup group : groups) {
            int count = valuesByGroup.getOrDefault(group.getId(), List.of()).size();
            if (count < group.getMinimumSelections() || count > group.getMaximumSelections()) {
                throw new AppException(ErrorCode.INVALID_OPTION_SELECTION);
            }
        }
    }

    private boolean isEffectivelyAvailable(BranchItem branchItem) {
        return Boolean.TRUE.equals(branchItem.getIsAvailable())
                && (branchItem.getSoldOutUntil() == null
                        || !branchItem.getSoldOutUntil().isAfter(Instant.now()));
    }
}
