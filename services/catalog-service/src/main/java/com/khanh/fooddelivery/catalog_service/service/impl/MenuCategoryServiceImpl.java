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

    @Override
    public MenuCategoryResponse create(UUID menuId, MenuCategoryCreateRequest request) {
        Menu menu = requiredMenu(menuId);
        authorize(menu);
        MenuCategory category = categoryMapper.toEntity(request);
        category.setMenu(menu);
        category.setStatus(CatalogStatus.ACTIVE);
        return categoryMapper.toResponse(categoryRepository.save(category));
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
        return categoryMapper.toResponse(categoryRepository.save(category));
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
    }

    private MenuCategoryResponse changeStatus(UUID menuId, UUID categoryId, CatalogStatus status) {
        MenuCategory category = requiredCategory(menuId, categoryId);
        authorize(category.getMenu());
        category.setStatus(status);
        return categoryMapper.toResponse(categoryRepository.save(category));
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
}
