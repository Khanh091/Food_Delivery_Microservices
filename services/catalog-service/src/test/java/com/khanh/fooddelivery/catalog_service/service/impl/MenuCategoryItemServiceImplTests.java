package com.khanh.fooddelivery.catalog_service.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.catalog_service.dto.request.MenuCategoryItemCreateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.MenuCategoryItemBatchCreateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.MenuCategoryItemSortOrderUpdateRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.MenuCategoryItemResponse;
import com.khanh.fooddelivery.catalog_service.entity.CatalogItem;
import com.khanh.fooddelivery.catalog_service.entity.BranchItem;
import com.khanh.fooddelivery.catalog_service.entity.Menu;
import com.khanh.fooddelivery.catalog_service.entity.MenuCategory;
import com.khanh.fooddelivery.catalog_service.entity.MenuCategoryItem;
import com.khanh.fooddelivery.catalog_service.exception.AppException;
import com.khanh.fooddelivery.catalog_service.exception.ErrorCode;
import com.khanh.fooddelivery.catalog_service.mapper.MenuCategoryItemMapper;
import com.khanh.fooddelivery.catalog_service.outbox.OutboxEventService;
import com.khanh.fooddelivery.catalog_service.repository.CatalogItemRepository;
import com.khanh.fooddelivery.catalog_service.repository.BranchItemRepository;
import com.khanh.fooddelivery.catalog_service.repository.MenuCategoryItemRepository;
import com.khanh.fooddelivery.catalog_service.repository.MenuCategoryRepository;
import com.khanh.fooddelivery.catalog_service.service.CatalogAuthorizationService;
import java.util.List;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MenuCategoryItemServiceImplTests {
    private final UUID restaurantId = UUID.randomUUID();
    private final UUID otherRestaurantId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();
    private final UUID menuId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();
    private final UUID itemId = UUID.randomUUID();

    @Mock private MenuCategoryRepository categoryRepository;
    @Mock private CatalogItemRepository itemRepository;
    @Mock private BranchItemRepository branchItemRepository;
    @Mock private MenuCategoryItemRepository categoryItemRepository;
    @Mock private MenuCategoryItemMapper categoryItemMapper;
    @Mock private CatalogAuthorizationService authorizationService;
    @Mock private OutboxEventService outboxEventService;

    private MenuCategoryItemServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new MenuCategoryItemServiceImpl(
                        categoryRepository,
                        itemRepository,
                        branchItemRepository,
                        categoryItemRepository,
                        categoryItemMapper,
                        authorizationService,
                        outboxEventService);
    }

    @Test
    void addValidItemAuthorizesCategoryMenuAndSavesMapping() {
        MenuCategory category = category();
        CatalogItem item = item(restaurantId);
        when(categoryRepository.findByIdAndMenuId(categoryId, menuId))
                .thenReturn(Optional.of(category));
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(categoryItemRepository.existsByCategoryIdAndItemId(categoryId, itemId))
                .thenReturn(false);
        when(categoryItemRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(categoryItemMapper.toResponse(any())).thenReturn(response());
        when(branchItemRepository.existsByBranchIdAndItemId(branchId, itemId)).thenReturn(true);

        service.add(menuId, categoryId, itemId, new MenuCategoryItemCreateRequest(2));

        verify(authorizationService).requireBranchCatalogAccess(restaurantId, branchId);
        verify(categoryItemRepository).save(any(MenuCategoryItem.class));
    }

    @Test
    void addCreatesMissingBranchItemAtCatalogBasePrice() {
        MenuCategory category = category();
        CatalogItem item = item(restaurantId);
        BranchItem createdBranchItem = new BranchItem();
        createdBranchItem.setId(UUID.randomUUID());
        createdBranchItem.setBranchId(branchId);
        createdBranchItem.setItem(item);
        createdBranchItem.setSellingPrice(item.getBasePrice());
        when(categoryRepository.findByIdAndMenuId(categoryId, menuId)).thenReturn(Optional.of(category));
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(categoryItemRepository.existsByCategoryIdAndItemId(categoryId, itemId)).thenReturn(false);
        when(categoryItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(categoryItemMapper.toResponse(any())).thenReturn(response());
        when(branchItemRepository.existsByBranchIdAndItemId(branchId, itemId)).thenReturn(false);
        when(branchItemRepository.save(any())).thenReturn(createdBranchItem);

        service.add(menuId, categoryId, itemId, new MenuCategoryItemCreateRequest(2));

        verify(branchItemRepository)
                .save(org.mockito.ArgumentMatchers.argThat(saved -> saved.getBranchId().equals(branchId)
                        && saved.getItem().equals(item)
                        && saved.getSellingPrice().compareTo(item.getBasePrice()) == 0
                        && Boolean.TRUE.equals(saved.getIsAvailable())));
    }

    @Test
    void addItemFromAnotherRestaurantIsRejectedBeforeAuthorization() {
        when(categoryRepository.findByIdAndMenuId(categoryId, menuId))
                .thenReturn(Optional.of(category()));
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item(otherRestaurantId)));

        AppException error =
                assertThrows(
                        AppException.class,
                        () ->
                                service.add(
                                        menuId,
                                        categoryId,
                                        itemId,
                                        new MenuCategoryItemCreateRequest(0)));

        assertEquals(ErrorCode.ITEM_CATEGORY_MISMATCH, error.getErrorCode());
        verify(categoryItemRepository, never()).save(any());
    }

    @Test
    void addDuplicateItemIsConflict() {
        when(categoryRepository.findByIdAndMenuId(categoryId, menuId))
                .thenReturn(Optional.of(category()));
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item(restaurantId)));
        when(categoryItemRepository.existsByCategoryIdAndItemId(categoryId, itemId))
                .thenReturn(true);

        AppException error =
                assertThrows(
                        AppException.class,
                        () ->
                                service.add(
                                        menuId,
                                        categoryId,
                                        itemId,
                                        new MenuCategoryItemCreateRequest(0)));

        assertEquals(ErrorCode.ITEM_ALREADY_IN_CATEGORY, error.getErrorCode());
    }

    @Test
    void listUsesCategoryMenuAuthorizationAndSortsInRepository() {
        when(categoryRepository.findByIdAndMenuId(categoryId, menuId))
                .thenReturn(Optional.of(category()));
        when(categoryItemRepository.findAllByCategoryIdOrderBySortOrderAsc(categoryId))
                .thenReturn(List.of());
        when(categoryItemMapper.toResponses(List.of())).thenReturn(List.of());

        service.list(menuId, categoryId);

        verify(authorizationService).requireBranchCatalogAccess(restaurantId, branchId);
        verify(categoryItemRepository).findAllByCategoryIdOrderBySortOrderAsc(categoryId);
    }

    @Test
    void categoryFromAnotherMenuIsNotFound() {
        when(categoryRepository.findByIdAndMenuId(categoryId, menuId)).thenReturn(Optional.empty());

        AppException error =
                assertThrows(AppException.class, () -> service.list(menuId, categoryId));

        assertEquals(ErrorCode.MENU_CATEGORY_NOT_FOUND, error.getErrorCode());
        verify(authorizationService, never()).requireBranchCatalogAccess(any(), any());
    }

    @Test
    void updateSortOrderUpdatesExistingMappingOnly() {
        MenuCategoryItem mapping = new MenuCategoryItem();
        mapping.setId(UUID.randomUUID());
        mapping.setCategory(category());
        mapping.setItem(item(restaurantId));
        when(categoryRepository.findByIdAndMenuId(categoryId, menuId))
                .thenReturn(Optional.of(category()));
        when(categoryItemRepository.findByCategoryIdAndItemId(categoryId, itemId))
                .thenReturn(Optional.of(mapping));
        when(categoryItemRepository.save(mapping)).thenReturn(mapping);
        when(categoryItemMapper.toResponse(mapping)).thenReturn(response());

        service.updateSortOrder(
                menuId, categoryId, itemId, new MenuCategoryItemSortOrderUpdateRequest(3));

        verify(categoryItemMapper)
                .update(
                        any(MenuCategoryItemSortOrderUpdateRequest.class),
                        org.mockito.ArgumentMatchers.same(mapping));
        verify(categoryItemRepository).save(mapping);
    }

    @Test
    void removeDeletesOnlyMapping() {
        MenuCategoryItem mapping = new MenuCategoryItem();
        mapping.setId(UUID.randomUUID());
        mapping.setCategory(category());
        mapping.setItem(item(restaurantId));
        when(categoryRepository.findByIdAndMenuId(categoryId, menuId))
                .thenReturn(Optional.of(category()));
        when(categoryItemRepository.findByCategoryIdAndItemId(categoryId, itemId))
                .thenReturn(Optional.of(mapping));

        service.remove(menuId, categoryId, itemId);

        verify(categoryItemRepository).delete(mapping);
        verify(itemRepository, never()).delete(any());
    }

    @Test
    void updateSortOrderRequiresMapping() {
        when(categoryRepository.findByIdAndMenuId(categoryId, menuId))
                .thenReturn(Optional.of(category()));
        when(categoryItemRepository.findByCategoryIdAndItemId(categoryId, itemId))
                .thenReturn(Optional.empty());

        AppException error =
                assertThrows(
                        AppException.class,
                        () ->
                                service.updateSortOrder(
                                        menuId,
                                        categoryId,
                                        itemId,
                                        new MenuCategoryItemSortOrderUpdateRequest(1)));

        assertEquals(ErrorCode.MENU_CATEGORY_ITEM_NOT_FOUND, error.getErrorCode());
    }

    @Test
    void addBatchAttachesAllItemsAndEnsuresTheirBranchSellingRecords() {
        UUID secondItemId = UUID.randomUUID();
        CatalogItem firstItem = item(restaurantId);
        CatalogItem secondItem = new CatalogItem();
        secondItem.setId(secondItemId);
        secondItem.setRestaurantId(restaurantId);
        secondItem.setBasePrice(new BigDecimal("50000"));
        when(categoryRepository.findByIdAndMenuId(categoryId, menuId)).thenReturn(Optional.of(category()));
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(firstItem));
        when(itemRepository.findById(secondItemId)).thenReturn(Optional.of(secondItem));
        when(categoryItemRepository.existsByCategoryIdAndItemId(categoryId, itemId)).thenReturn(false);
        when(categoryItemRepository.existsByCategoryIdAndItemId(categoryId, secondItemId)).thenReturn(false);
        when(categoryItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(categoryItemMapper.toResponses(any())).thenReturn(List.of());
        when(branchItemRepository.existsByBranchIdAndItemId(branchId, itemId)).thenReturn(true);
        when(branchItemRepository.existsByBranchIdAndItemId(branchId, secondItemId)).thenReturn(true);

        service.addBatch(menuId, categoryId, new MenuCategoryItemBatchCreateRequest(List.of(itemId, secondItemId)));

        verify(authorizationService).requireBranchCatalogAccess(restaurantId, branchId);
        verify(categoryItemRepository).saveAll(any());
        verify(branchItemRepository).existsByBranchIdAndItemId(branchId, itemId);
        verify(branchItemRepository).existsByBranchIdAndItemId(branchId, secondItemId);
    }

    @Test
    void addBatchRejectsDuplicateRequestIdsBeforeCreatingMappings() {
        when(categoryRepository.findByIdAndMenuId(categoryId, menuId)).thenReturn(Optional.of(category()));

        AppException error = assertThrows(
                AppException.class,
                () -> service.addBatch(menuId, categoryId, new MenuCategoryItemBatchCreateRequest(List.of(itemId, itemId))));

        assertEquals(ErrorCode.INVALID_REQUEST, error.getErrorCode());
        verify(categoryItemRepository, never()).saveAll(any());
    }

    private MenuCategory category() {
        Menu menu = new Menu();
        menu.setId(menuId);
        menu.setRestaurantId(restaurantId);
        menu.setBranchId(branchId);
        MenuCategory category = new MenuCategory();
        category.setId(categoryId);
        category.setMenu(menu);
        return category;
    }

    private CatalogItem item(UUID itemRestaurantId) {
        CatalogItem item = new CatalogItem();
        item.setId(itemId);
        item.setRestaurantId(itemRestaurantId);
        item.setBasePrice(new BigDecimal("42000"));
        return item;
    }

    private MenuCategoryItemResponse response() {
        return new MenuCategoryItemResponse(null, categoryId, itemId, 2, null, null);
    }
}
