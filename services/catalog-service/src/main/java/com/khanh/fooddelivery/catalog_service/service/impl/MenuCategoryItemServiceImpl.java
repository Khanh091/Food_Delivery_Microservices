package com.khanh.fooddelivery.catalog_service.service.impl;

import com.khanh.fooddelivery.catalog_service.dto.request.MenuCategoryItemCreateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.MenuCategoryItemSortOrderUpdateRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.MenuCategoryItemResponse;
import com.khanh.fooddelivery.catalog_service.entity.CatalogItem;
import com.khanh.fooddelivery.catalog_service.entity.Menu;
import com.khanh.fooddelivery.catalog_service.entity.MenuCategory;
import com.khanh.fooddelivery.catalog_service.entity.MenuCategoryItem;
import com.khanh.fooddelivery.catalog_service.exception.AppException;
import com.khanh.fooddelivery.catalog_service.exception.ErrorCode;
import com.khanh.fooddelivery.catalog_service.mapper.MenuCategoryItemMapper;
import com.khanh.fooddelivery.catalog_service.outbox.CatalogEventData;
import com.khanh.fooddelivery.catalog_service.outbox.CatalogEventType;
import com.khanh.fooddelivery.catalog_service.outbox.OutboxEventService;
import com.khanh.fooddelivery.catalog_service.repository.CatalogItemRepository;
import com.khanh.fooddelivery.catalog_service.repository.MenuCategoryItemRepository;
import com.khanh.fooddelivery.catalog_service.repository.MenuCategoryRepository;
import com.khanh.fooddelivery.catalog_service.service.CatalogAuthorizationService;
import com.khanh.fooddelivery.catalog_service.service.MenuCategoryItemService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MenuCategoryItemServiceImpl implements MenuCategoryItemService {
    private final MenuCategoryRepository categoryRepository;
    private final CatalogItemRepository itemRepository;
    private final MenuCategoryItemRepository categoryItemRepository;
    private final MenuCategoryItemMapper categoryItemMapper;
    private final CatalogAuthorizationService authorizationService;
    private final OutboxEventService outboxEventService;

    @Override
    public MenuCategoryItemResponse add(
            UUID menuId, UUID categoryId, UUID itemId, MenuCategoryItemCreateRequest request) {
        MenuCategory category = requiredCategory(menuId, categoryId);
        CatalogItem item = requiredItem(itemId);
        Menu menu = category.getMenu();
        verifyItemRestaurant(menu, item);
        authorize(menu);
        if (categoryItemRepository.existsByCategoryIdAndItemId(categoryId, itemId)) {
            throw new AppException(ErrorCode.ITEM_ALREADY_IN_CATEGORY);
        }

        MenuCategoryItem categoryItem = new MenuCategoryItem();
        categoryItem.setCategory(category);
        categoryItem.setItem(item);
        categoryItem.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        MenuCategoryItem savedCategoryItem = categoryItemRepository.save(categoryItem);
        enqueue(savedCategoryItem, "ATTACHED");
        return categoryItemMapper.toResponse(savedCategoryItem);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuCategoryItemResponse> list(UUID menuId, UUID categoryId) {
        MenuCategory category = requiredCategory(menuId, categoryId);
        authorize(category.getMenu());
        return categoryItemMapper.toResponses(
                categoryItemRepository.findAllByCategoryIdOrderBySortOrderAsc(categoryId));
    }

    @Override
    public MenuCategoryItemResponse updateSortOrder(
            UUID menuId,
            UUID categoryId,
            UUID itemId,
            MenuCategoryItemSortOrderUpdateRequest request) {
        MenuCategory category = requiredCategory(menuId, categoryId);
        authorize(category.getMenu());
        MenuCategoryItem categoryItem = requiredCategoryItem(categoryId, itemId);
        categoryItemMapper.update(request, categoryItem);
        MenuCategoryItem savedCategoryItem = categoryItemRepository.save(categoryItem);
        enqueue(savedCategoryItem, "SORT_ORDER_UPDATED");
        return categoryItemMapper.toResponse(savedCategoryItem);
    }

    @Override
    public void remove(UUID menuId, UUID categoryId, UUID itemId) {
        MenuCategory category = requiredCategory(menuId, categoryId);
        authorize(category.getMenu());
        MenuCategoryItem categoryItem = requiredCategoryItem(categoryId, itemId);
        categoryItemRepository.delete(categoryItem);
        enqueue(categoryItem, "REMOVED");
    }

    private MenuCategory requiredCategory(UUID menuId, UUID categoryId) {
        return categoryRepository
                .findByIdAndMenuId(categoryId, menuId)
                .orElseThrow(() -> new AppException(ErrorCode.MENU_CATEGORY_NOT_FOUND));
    }

    private CatalogItem requiredItem(UUID itemId) {
        return itemRepository
                .findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.CATALOG_ITEM_NOT_FOUND));
    }

    private MenuCategoryItem requiredCategoryItem(UUID categoryId, UUID itemId) {
        return categoryItemRepository
                .findByCategoryIdAndItemId(categoryId, itemId)
                .orElseThrow(() -> new AppException(ErrorCode.MENU_CATEGORY_ITEM_NOT_FOUND));
    }

    private void verifyItemRestaurant(Menu menu, CatalogItem item) {
        if (!menu.getRestaurantId().equals(item.getRestaurantId())) {
            throw new AppException(ErrorCode.ITEM_CATEGORY_MISMATCH);
        }
    }

    private void authorize(Menu menu) {
        authorizationService.requireBranchCatalogAccess(menu.getRestaurantId(), menu.getBranchId());
    }

    private void enqueue(MenuCategoryItem categoryItem, String action) {
        outboxEventService.enqueue(
                CatalogEventType.MENU_CATEGORY_ITEM_CHANGED,
                "MENU_CATEGORY_ITEM",
                categoryItem.getId(),
                CatalogEventData.menuCategoryItem(categoryItem, action));
    }
}
