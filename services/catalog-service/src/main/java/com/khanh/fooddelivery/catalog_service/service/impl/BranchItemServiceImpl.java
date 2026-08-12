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
import com.khanh.fooddelivery.catalog_service.outbox.CatalogEventData;
import com.khanh.fooddelivery.catalog_service.outbox.CatalogEventType;
import com.khanh.fooddelivery.catalog_service.outbox.OutboxEventService;
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
    private final OutboxEventService outboxEventService;

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
        BranchItem savedBranchItem = branchItemRepository.save(branchItem);
        enqueue(CatalogEventType.BRANCH_ITEM_UPSERTED, savedBranchItem, "CREATED");
        return branchItemMapper.toResponse(savedBranchItem);
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
        BranchItem branchItem = requiredBranchItemForUpdate(branchItemId);
        authorize(branchItem);

        boolean priceChanged = branchItem.getSellingPrice().compareTo(request.sellingPrice()) != 0;
        boolean originalPriceChanged =
                request.originalPrice() != null
                        && !request.originalPrice().equals(branchItem.getOriginalPrice());
        if (priceChanged) {
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
        BranchItem savedBranchItem = branchItemRepository.save(branchItem);
        if (priceChanged) {
            enqueue(CatalogEventType.BRANCH_ITEM_PRICE_CHANGED, savedBranchItem, "PRICE_UPDATED");
        } else if (originalPriceChanged) {
            enqueue(
                    CatalogEventType.BRANCH_ITEM_UPSERTED,
                    savedBranchItem,
                    "ORIGINAL_PRICE_UPDATED");
        }
        return branchItemMapper.toResponse(savedBranchItem);
    }

    @Override
    public BranchItemResponse markAvailable(UUID branchItemId) {
        BranchItem branchItem = requiredBranchItemForUpdate(branchItemId);
        authorize(branchItem);
        boolean changed =
                !Boolean.TRUE.equals(branchItem.getIsAvailable())
                        || branchItem.getSoldOutUntil() != null;
        branchItem.setIsAvailable(true);
        branchItem.setSoldOutUntil(null);
        BranchItem savedBranchItem = branchItemRepository.save(branchItem);
        if (changed) {
            enqueue(
                    CatalogEventType.BRANCH_ITEM_AVAILABILITY_CHANGED,
                    savedBranchItem,
                    "AVAILABLE");
        }
        return branchItemMapper.toResponse(savedBranchItem);
    }

    @Override
    public BranchItemResponse markUnavailable(UUID branchItemId) {
        BranchItem branchItem = requiredBranchItemForUpdate(branchItemId);
        authorize(branchItem);
        boolean changed = !Boolean.FALSE.equals(branchItem.getIsAvailable());
        branchItem.setIsAvailable(false);
        BranchItem savedBranchItem = branchItemRepository.save(branchItem);
        if (changed) {
            enqueue(
                    CatalogEventType.BRANCH_ITEM_AVAILABILITY_CHANGED,
                    savedBranchItem,
                    "UNAVAILABLE");
        }
        return branchItemMapper.toResponse(savedBranchItem);
    }

    @Override
    public BranchItemResponse markSoldOut(UUID branchItemId, BranchItemSoldOutRequest request) {
        if (!request.soldOutUntil().isAfter(Instant.now())) {
            throw new AppException(ErrorCode.INVALID_SOLD_OUT_TIME);
        }
        BranchItem branchItem = requiredBranchItemForUpdate(branchItemId);
        authorize(branchItem);
        boolean changed =
                !Boolean.FALSE.equals(branchItem.getIsAvailable())
                        || !request.soldOutUntil().equals(branchItem.getSoldOutUntil());
        branchItem.setIsAvailable(false);
        branchItem.setSoldOutUntil(request.soldOutUntil());
        BranchItem savedBranchItem = branchItemRepository.save(branchItem);
        if (changed) {
            enqueue(CatalogEventType.BRANCH_ITEM_AVAILABILITY_CHANGED, savedBranchItem, "SOLD_OUT");
        }
        return branchItemMapper.toResponse(savedBranchItem);
    }

    @Override
    public BranchItemResponse updateQuantity(
            UUID branchItemId, BranchItemQuantityUpdateRequest request) {
        BranchItem branchItem = requiredBranchItemForUpdate(branchItemId);
        authorize(branchItem);
        boolean changed = !request.availableQuantity().equals(branchItem.getAvailableQuantity());
        branchItem.setAvailableQuantity(request.availableQuantity());
        BranchItem savedBranchItem = branchItemRepository.save(branchItem);
        if (changed) {
            enqueue(CatalogEventType.BRANCH_ITEM_UPSERTED, savedBranchItem, "QUANTITY_UPDATED");
        }
        return branchItemMapper.toResponse(savedBranchItem);
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

    private BranchItem requiredBranchItemForUpdate(UUID branchItemId) {
        return branchItemRepository
                .findByIdForUpdate(branchItemId)
                .orElseThrow(() -> new AppException(ErrorCode.BRANCH_ITEM_NOT_FOUND));
    }

    private void authorize(BranchItem branchItem) {
        authorizationService.requireBranchCatalogAccess(
                branchItem.getItem().getRestaurantId(), branchItem.getBranchId());
    }

    private void enqueue(CatalogEventType eventType, BranchItem branchItem, String action) {
        outboxEventService.enqueue(
                eventType,
                "BRANCH_ITEM",
                branchItem.getId(),
                CatalogEventData.branchItem(branchItem, action));
    }
}
