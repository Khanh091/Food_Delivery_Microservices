package com.khanh.fooddelivery.catalog_service.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MenuCategoryServiceImplTests {
    private final UUID menuId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();
    private final UUID restaurantId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();
    @Mock private MenuRepository menuRepository;
    @Mock private MenuCategoryRepository categoryRepository;
    @Mock private MenuCategoryMapper categoryMapper;
    @Mock private CatalogAuthorizationService authorizationService;
    private MenuCategoryServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new MenuCategoryServiceImpl(
                        menuRepository, categoryRepository, categoryMapper, authorizationService);
    }

    @Test
    void createUsesParentMenuAuthorization() {
        Menu menu = menu();
        MenuCategory category = category(menu);
        when(menuRepository.findById(menuId)).thenReturn(Optional.of(menu));
        when(categoryMapper.toEntity(any())).thenReturn(category);
        when(categoryRepository.save(category)).thenReturn(category);
        when(categoryMapper.toResponse(category)).thenReturn(response(category));

        service.create(menuId, new MenuCategoryCreateRequest("Main", null, 0));

        verify(authorizationService).requireBranchCatalogAccess(restaurantId, branchId);
        assertEquals(menu, category.getMenu());
        assertEquals(CatalogStatus.ACTIVE, category.getStatus());
    }

    @Test
    void categoryFromWrongMenuIsNotFound() {
        when(categoryRepository.findByIdAndMenuId(categoryId, menuId)).thenReturn(Optional.empty());
        AppException error =
                assertThrows(AppException.class, () -> service.get(menuId, categoryId));
        assertEquals(ErrorCode.MENU_CATEGORY_NOT_FOUND, error.getErrorCode());
    }

    @Test
    void updatePreservesParentMenu() {
        Menu menu = menu();
        MenuCategory category = category(menu);
        when(categoryRepository.findByIdAndMenuId(categoryId, menuId))
                .thenReturn(Optional.of(category));
        when(categoryRepository.save(category)).thenReturn(category);
        when(categoryMapper.toResponse(category)).thenReturn(response(category));

        service.update(menuId, categoryId, new MenuCategoryUpdateRequest("Updated", null, 1));

        assertEquals(menu, category.getMenu());
    }

    @Test
    void listUsesSortOrderRepositoryMethod() {
        Menu menu = menu();
        MenuCategory first = category(menu);
        first.setSortOrder(0);
        MenuCategory second = category(menu);
        second.setSortOrder(1);
        when(menuRepository.findById(menuId)).thenReturn(Optional.of(menu));
        when(categoryRepository.findAllByMenuIdOrderBySortOrderAsc(menuId))
                .thenReturn(List.of(first, second));
        when(categoryMapper.toResponses(List.of(first, second)))
                .thenReturn(List.of(response(first), response(second)));

        assertEquals(2, service.list(menuId).size());
        verify(categoryRepository).findAllByMenuIdOrderBySortOrderAsc(menuId);
    }

    private Menu menu() {
        Menu menu = new Menu();
        menu.setId(menuId);
        menu.setRestaurantId(restaurantId);
        menu.setBranchId(branchId);
        return menu;
    }

    private MenuCategory category(Menu menu) {
        MenuCategory category = new MenuCategory();
        category.setId(categoryId);
        category.setMenu(menu);
        return category;
    }

    private MenuCategoryResponse response(MenuCategory category) {
        return new MenuCategoryResponse(
                category.getId(),
                menuId,
                "Main",
                null,
                category.getSortOrder(),
                category.getStatus(),
                null,
                null);
    }
}
