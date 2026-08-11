package com.khanh.fooddelivery.catalog_service.service.impl;

import com.khanh.fooddelivery.catalog_service.dto.request.BranchItemCreateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.BranchItemPriceUpdateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.BranchItemQuantityUpdateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.BranchItemSoldOutRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.BranchItemResponse;
import com.khanh.fooddelivery.catalog_service.dto.response.ItemPriceHistoryResponse;
import com.khanh.fooddelivery.catalog_service.entity.BranchItem;
import com.khanh.fooddelivery.catalog_service.entity.CatalogItem;
import com.khanh.fooddelivery.catalog_service.entity.ItemPriceHistory;
import com.khanh.fooddelivery.catalog_service.exception.AppException;
import com.khanh.fooddelivery.catalog_service.exception.ErrorCode;
import com.khanh.fooddelivery.catalog_service.mapper.BranchItemMapper;
import com.khanh.fooddelivery.catalog_service.mapper.ItemPriceHistoryMapper;
import com.khanh.fooddelivery.catalog_service.repository.BranchItemRepository;
import com.khanh.fooddelivery.catalog_service.repository.CatalogItemRepository;
import com.khanh.fooddelivery.catalog_service.repository.ItemPriceHistoryRepository;
import com.khanh.fooddelivery.catalog_service.security.SecurityAuditorAware;
import com.khanh.fooddelivery.catalog_service.service.BranchItemService;
import com.khanh.fooddelivery.catalog_service.service.CatalogAuthorizationService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BranchItemServiceImpl implements BranchItemService {
    private final BranchItemRepository branchItemRepository;
    private final CatalogItemRepository itemRepository;
    private final ItemPriceHistoryRepository priceHistoryRepository;
    private final BranchItemMapper branchItemMapper;
    private final ItemPriceHistoryMapper priceHistoryMapper;
    private final CatalogAuthorizationService authorizationService;
    private final SecurityAuditorAware auditorAware;

    @Override
    public BranchItemResponse create(BranchItemCreateRequest request) {
        CatalogItem item = requiredItem(request.itemId());
        authorizationService.requireBranchCatalogAccess(item.getRestaurantId(), request.branchId());
        if (branchItemRepository.existsByBranchIdAndItemId(request.branchId(), request.itemId())) {
            throw new AppException(ErrorCode.BRANCH_ITEM_ALREADY_EXISTS);
        }

        BranchItem branchItem = branchItemMapper.toEntity(request);
        branchItem.setItem(item);
        branchItem.setIsAvailable(true);
        branchItem.setSoldOutUntil(null);
        return branchItemMapper.toResponse(branchItemRepository.save(branchItem));
    }

    @Override
    @Transactional(readOnly = true)
    public BranchItemResponse get(UUID branchItemId) {
        BranchItem branchItem = requiredBranchItem(branchItemId);
        authorize(branchItem);
        return branchItemMapper.toResponse(branchItem);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchItemResponse> listByBranch(UUID restaurantId, UUID branchId) {
        authorizationService.requireBranchCatalogAccess(restaurantId, branchId);
        List<BranchItem> branchItems = branchItemRepository.findAllByBranchId(branchId);
        if (branchItems.stream()
                .anyMatch(
                        branchItem ->
                                !restaurantId.equals(branchItem.getItem().getRestaurantId()))) {
            throw new AppException(
                    ErrorCode.DATA_CONFLICT, "Branch item restaurant scope is inconsistent");
        }
        return branchItemMapper.toResponses(branchItems);
    }

    @Override
    public BranchItemResponse updatePrice(UUID branchItemId, BranchItemPriceUpdateRequest request) {
        BranchItem branchItem = requiredBranchItem(branchItemId);
        authorize(branchItem);

        if (branchItem.getSellingPrice().compareTo(request.sellingPrice()) != 0) {
            ItemPriceHistory history = new ItemPriceHistory();
            history.setBranchItem(branchItem);
            history.setOldPrice(branchItem.getSellingPrice());
            history.setNewPrice(request.sellingPrice());
            history.setReason(request.reason());
            history.setChangedBy(auditorAware.getCurrentAuditor().orElse(null));
            branchItem.setSellingPrice(request.sellingPrice());
            priceHistoryRepository.save(history);
        }
        if (request.originalPrice() != null) {
            branchItem.setOriginalPrice(request.originalPrice());
        }
        return branchItemMapper.toResponse(branchItemRepository.save(branchItem));
    }

    @Override
    public BranchItemResponse markAvailable(UUID branchItemId) {
        BranchItem branchItem = requiredBranchItem(branchItemId);
        authorize(branchItem);
        branchItem.setIsAvailable(true);
        branchItem.setSoldOutUntil(null);
        return branchItemMapper.toResponse(branchItemRepository.save(branchItem));
    }

    @Override
    public BranchItemResponse markUnavailable(UUID branchItemId) {
        BranchItem branchItem = requiredBranchItem(branchItemId);
        authorize(branchItem);
        branchItem.setIsAvailable(false);
        return branchItemMapper.toResponse(branchItemRepository.save(branchItem));
    }

    @Override
    public BranchItemResponse markSoldOut(UUID branchItemId, BranchItemSoldOutRequest request) {
        if (!request.soldOutUntil().isAfter(Instant.now())) {
            throw new AppException(ErrorCode.INVALID_SOLD_OUT_TIME);
        }
        BranchItem branchItem = requiredBranchItem(branchItemId);
        authorize(branchItem);
        branchItem.setIsAvailable(false);
        branchItem.setSoldOutUntil(request.soldOutUntil());
        return branchItemMapper.toResponse(branchItemRepository.save(branchItem));
    }

    @Override
    public BranchItemResponse updateQuantity(
            UUID branchItemId, BranchItemQuantityUpdateRequest request) {
        BranchItem branchItem = requiredBranchItem(branchItemId);
        authorize(branchItem);
        branchItem.setAvailableQuantity(request.availableQuantity());
        return branchItemMapper.toResponse(branchItemRepository.save(branchItem));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemPriceHistoryResponse> getPriceHistory(UUID branchItemId) {
        BranchItem branchItem = requiredBranchItem(branchItemId);
        authorize(branchItem);
        return priceHistoryMapper.toResponses(
                priceHistoryRepository.findAllByBranchItemIdOrderByCreatedAtDesc(branchItemId));
    }

    private CatalogItem requiredItem(UUID itemId) {
        return itemRepository
                .findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.CATALOG_ITEM_NOT_FOUND));
    }

    private BranchItem requiredBranchItem(UUID branchItemId) {
        return branchItemRepository
                .findById(branchItemId)
                .orElseThrow(() -> new AppException(ErrorCode.BRANCH_ITEM_NOT_FOUND));
    }

    private void authorize(BranchItem branchItem) {
        authorizationService.requireBranchCatalogAccess(
                branchItem.getItem().getRestaurantId(), branchItem.getBranchId());
    }
}
