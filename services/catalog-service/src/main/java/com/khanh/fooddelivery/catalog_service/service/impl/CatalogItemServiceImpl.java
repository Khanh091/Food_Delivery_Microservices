package com.khanh.fooddelivery.catalog_service.service.impl;

import com.khanh.fooddelivery.catalog_service.dto.request.CatalogItemCreateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.CatalogItemUpdateRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.CatalogItemResponse;
import com.khanh.fooddelivery.catalog_service.entity.CatalogItem;
import com.khanh.fooddelivery.catalog_service.enums.CatalogStatus;
import com.khanh.fooddelivery.catalog_service.exception.AppException;
import com.khanh.fooddelivery.catalog_service.exception.ErrorCode;
import com.khanh.fooddelivery.catalog_service.mapper.CatalogItemMapper;
import com.khanh.fooddelivery.catalog_service.repository.CatalogItemRepository;
import com.khanh.fooddelivery.catalog_service.service.CatalogAuthorizationService;
import com.khanh.fooddelivery.catalog_service.service.CatalogItemService;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CatalogItemServiceImpl implements CatalogItemService {
    private static final String DEFAULT_CURRENCY = "VND";

    private final CatalogItemRepository itemRepository;
    private final CatalogItemMapper itemMapper;
    private final CatalogAuthorizationService authorizationService;

    @Override
    public CatalogItemResponse create(CatalogItemCreateRequest request) {
        authorizationService.requireRestaurantCatalogAccess(request.restaurantId());

        CatalogItem item = itemMapper.toEntity(request);
        item.setStatus(CatalogStatus.ACTIVE);
        item.setCurrency(normalizeCurrency(request.currency()));
        item.setIsVegetarian(Boolean.TRUE.equals(request.isVegetarian()));
        return itemMapper.toResponse(itemRepository.save(item));
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogItemResponse get(UUID itemId) {
        CatalogItem item = requiredItem(itemId);
        authorize(item);
        return itemMapper.toResponse(item);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogItemResponse> list(UUID restaurantId) {
        authorizationService.requireRestaurantCatalogAccess(restaurantId);
        return itemMapper.toResponses(
                itemRepository.findAllByRestaurantIdOrderByCreatedAtAsc(restaurantId));
    }

    @Override
    public CatalogItemResponse update(UUID itemId, CatalogItemUpdateRequest request) {
        CatalogItem item = requiredItem(itemId);
        authorize(item);
        itemMapper.update(request, item);
        if (request.currency() != null) {
            item.setCurrency(normalizeCurrency(request.currency()));
        }
        return itemMapper.toResponse(itemRepository.save(item));
    }

    @Override
    public CatalogItemResponse activate(UUID itemId) {
        return changeStatus(itemId, CatalogStatus.ACTIVE);
    }

    @Override
    public CatalogItemResponse deactivate(UUID itemId) {
        return changeStatus(itemId, CatalogStatus.INACTIVE);
    }

    private CatalogItemResponse changeStatus(UUID itemId, CatalogStatus status) {
        CatalogItem item = requiredItem(itemId);
        authorize(item);
        item.setStatus(status);
        return itemMapper.toResponse(itemRepository.save(item));
    }

    private CatalogItem requiredItem(UUID itemId) {
        return itemRepository
                .findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.CATALOG_ITEM_NOT_FOUND));
    }

    private void authorize(CatalogItem item) {
        authorizationService.requireRestaurantCatalogAccess(item.getRestaurantId());
    }

    private String normalizeCurrency(String currency) {
        return currency == null ? DEFAULT_CURRENCY : currency.toUpperCase(Locale.ROOT);
    }
}
