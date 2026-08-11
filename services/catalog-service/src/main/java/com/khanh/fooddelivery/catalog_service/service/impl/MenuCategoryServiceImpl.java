package com.khanh.fooddelivery.catalog_service.service.impl;

import com.khanh.fooddelivery.catalog_service.dto.request.MenuCategoryCreateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.MenuCategoryUpdateRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.MenuCategoryResponse;
import com.khanh.fooddelivery.catalog_service.entity.Menu;
import com.khanh.fooddelivery.catalog_service.entity.MenuCategory;
import com.khanh.fooddelivery.catalog_service.enums.CatalogStatus;
import com.khanh.fooddelivery.catalog_service.exception.AppException;
import com.khanh.fooddelivery.catalog_service.exception.ErrorCode;
import com.khanh.fooddelivery.catalog_service.mapper.MenuCategoryMapper;
import com.khanh.fooddelivery.catalog_service.outbox.CatalogEventData;
import com.khanh.fooddelivery.catalog_service.outbox.CatalogEventType;
import com.khanh.fooddelivery.catalog_service.outbox.OutboxEventService;
import com.khanh.fooddelivery.catalog_service.repository.MenuCategoryRepository;
import com.khanh.fooddelivery.catalog_service.repository.MenuRepository;
import com.khanh.fooddelivery.catalog_service.service.CatalogAuthorizationService;
import com.khanh.fooddelivery.catalog_service.service.MenuCategoryService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MenuCategoryServiceImpl implements MenuCategoryService {
    private final MenuRepository menuRepository;
    private final MenuCategoryRepository categoryRepository;
    private final MenuCategoryMapper categoryMapper;
    private final CatalogAuthorizationService authorizationService;
    private final OutboxEventService outboxEventService;

    @Override
    public MenuCategoryResponse create(UUID menuId, MenuCategoryCreateRequest request) {
        Menu menu = requiredMenu(menuId);
        authorize(menu);
        MenuCategory category = categoryMapper.toEntity(request);
        category.setMenu(menu);
        category.setStatus(CatalogStatus.ACTIVE);
        MenuCategory savedCategory = categoryRepository.save(category);
        enqueue(savedCategory, "CREATED");
        return categoryMapper.toResponse(savedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public MenuCategoryResponse get(UUID menuId, UUID categoryId) {
        MenuCategory category = requiredCategory(menuId, categoryId);
        authorize(category.getMenu());
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuCategoryResponse> list(UUID menuId) {
        Menu menu = requiredMenu(menuId);
        authorize(menu);
        return categoryMapper.toResponses(
                categoryRepository.findAllByMenuIdOrderBySortOrderAsc(menuId));
    }

    @Override
    public MenuCategoryResponse update(
            UUID menuId, UUID categoryId, MenuCategoryUpdateRequest request) {
        MenuCategory category = requiredCategory(menuId, categoryId);
        authorize(category.getMenu());
        categoryMapper.update(request, category);
        MenuCategory savedCategory = categoryRepository.save(category);
        enqueue(savedCategory, "UPDATED");
        return categoryMapper.toResponse(savedCategory);
    }

    @Override
    public MenuCategoryResponse activate(UUID menuId, UUID categoryId) {
        return changeStatus(menuId, categoryId, CatalogStatus.ACTIVE);
    }

    @Override
    public MenuCategoryResponse deactivate(UUID menuId, UUID categoryId) {
        return changeStatus(menuId, categoryId, CatalogStatus.INACTIVE);
    }

    @Override
    public void delete(UUID menuId, UUID categoryId) {
        MenuCategory category = requiredCategory(menuId, categoryId);
        authorize(category.getMenu());
        categoryRepository.delete(category);
        enqueue(category, "DELETED");
    }

    private MenuCategoryResponse changeStatus(UUID menuId, UUID categoryId, CatalogStatus status) {
        MenuCategory category = requiredCategory(menuId, categoryId);
        authorize(category.getMenu());
        if (category.getStatus() == status) {
            return categoryMapper.toResponse(category);
        }
        category.setStatus(status);
        MenuCategory savedCategory = categoryRepository.save(category);
        enqueue(savedCategory, status.name());
        return categoryMapper.toResponse(savedCategory);
    }

    private Menu requiredMenu(UUID menuId) {
        return menuRepository
                .findById(menuId)
                .orElseThrow(() -> new AppException(ErrorCode.MENU_NOT_FOUND));
    }

    private MenuCategory requiredCategory(UUID menuId, UUID categoryId) {
        return categoryRepository
                .findByIdAndMenuId(categoryId, menuId)
                .orElseThrow(() -> new AppException(ErrorCode.MENU_CATEGORY_NOT_FOUND));
    }

    private void authorize(Menu menu) {
        authorizationService.requireBranchCatalogAccess(menu.getRestaurantId(), menu.getBranchId());
    }

    private void enqueue(MenuCategory category, String action) {
        outboxEventService.enqueue(
                CatalogEventType.MENU_CATEGORY_CHANGED,
                "MENU_CATEGORY",
                category.getId(),
                CatalogEventData.menuCategory(category, action));
    }
}
