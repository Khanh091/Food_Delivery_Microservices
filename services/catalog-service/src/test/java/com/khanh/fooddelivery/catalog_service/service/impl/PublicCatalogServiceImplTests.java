package com.khanh.fooddelivery.catalog_service.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.catalog_service.client.RestaurantServiceClient;
import com.khanh.fooddelivery.catalog_service.common.response.ApiResponse;
import com.khanh.fooddelivery.catalog_service.dto.response.publiccatalog.PublicCatalogItemResponse;
import com.khanh.fooddelivery.catalog_service.dto.response.publiccatalog.PublicCatalogResponse;
import com.khanh.fooddelivery.catalog_service.entity.BranchItem;
import com.khanh.fooddelivery.catalog_service.entity.CatalogItem;
import com.khanh.fooddelivery.catalog_service.entity.Menu;
import com.khanh.fooddelivery.catalog_service.entity.MenuCategory;
import com.khanh.fooddelivery.catalog_service.entity.MenuCategoryItem;
import com.khanh.fooddelivery.catalog_service.entity.OptionGroup;
import com.khanh.fooddelivery.catalog_service.entity.OptionValue;
import com.khanh.fooddelivery.catalog_service.enums.CatalogItemType;
import com.khanh.fooddelivery.catalog_service.enums.CatalogStatus;
import com.khanh.fooddelivery.catalog_service.enums.OptionSelectionType;
import com.khanh.fooddelivery.catalog_service.exception.AppException;
import com.khanh.fooddelivery.catalog_service.exception.ErrorCode;
import com.khanh.fooddelivery.catalog_service.repository.BranchItemRepository;
import com.khanh.fooddelivery.catalog_service.repository.CatalogItemRepository;
import com.khanh.fooddelivery.catalog_service.repository.ItemImageRepository;
import com.khanh.fooddelivery.catalog_service.repository.MenuCategoryItemRepository;
import com.khanh.fooddelivery.catalog_service.repository.MenuCategoryRepository;
import com.khanh.fooddelivery.catalog_service.repository.MenuRepository;
import com.khanh.fooddelivery.catalog_service.repository.OptionGroupRepository;
import com.khanh.fooddelivery.catalog_service.repository.OptionValueRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublicCatalogServiceImplTests {
    private final UUID restaurantId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();
    private final UUID menuId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();
    private final UUID itemId = UUID.randomUUID();

    @Mock private RestaurantServiceClient restaurantServiceClient;
    @Mock private MenuRepository menuRepository;
    @Mock private MenuCategoryRepository categoryRepository;
    @Mock private MenuCategoryItemRepository categoryItemRepository;
    @Mock private CatalogItemRepository itemRepository;
    @Mock private BranchItemRepository branchItemRepository;
    @Mock private ItemImageRepository imageRepository;
    @Mock private OptionGroupRepository optionGroupRepository;
    @Mock private OptionValueRepository optionValueRepository;

    private PublicCatalogServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new PublicCatalogServiceImpl(
                        restaurantServiceClient,
                        menuRepository,
                        categoryRepository,
                        categoryItemRepository,
                        itemRepository,
                        branchItemRepository,
                        imageRepository,
                        optionGroupRepository,
                        optionValueRepository);
        when(restaurantServiceClient.getPublicBranchAvailability(restaurantId, branchId))
                .thenReturn(
                        ApiResponse.success(
                                "available",
                                new RestaurantServiceClient.PublicBranchAvailabilityResponse(
                                        restaurantId, branchId, true, true)));
    }

    @Test
    void branchCatalogBuildsVisibleItemsInCategoryMappingOrder() {
        Menu menu = menu();
        MenuCategory category = category(menu);
        CatalogItem item = item();
        MenuCategoryItem mapping = mapping(category, item);
        BranchItem branchItem = branchItem(item, true, null);
        OptionGroup group = optionGroup(item);
        OptionValue value = optionValue(group);

        when(menuRepository.findAllByRestaurantIdAndBranchIdAndStatusOrderByCreatedAtAsc(
                        restaurantId, branchId, CatalogStatus.ACTIVE))
                .thenReturn(List.of(menu));
        when(categoryRepository.findAllByMenuIdInAndStatusOrderBySortOrderAsc(
                        List.of(menuId), CatalogStatus.ACTIVE))
                .thenReturn(List.of(category));
        when(categoryItemRepository.findAllByCategoryIdInOrderBySortOrderAsc(List.of(categoryId)))
                .thenReturn(List.of(mapping));
        when(itemRepository.findAllByIdInAndStatus(List.of(itemId), CatalogStatus.ACTIVE))
                .thenReturn(List.of(item));
        when(branchItemRepository.findAllByBranchIdAndItemIdIn(branchId, List.of(itemId)))
                .thenReturn(List.of(branchItem));
        when(imageRepository.findAllByItemIdInOrderByIsPrimaryDescSortOrderAscCreatedAtAsc(
                        List.of(itemId)))
                .thenReturn(List.of());
        when(optionGroupRepository.findAllByItemIdInAndStatusOrderBySortOrderAsc(
                        List.of(itemId), CatalogStatus.ACTIVE))
                .thenReturn(List.of(group));
        when(optionValueRepository.findAllByOptionGroupIdInAndIsAvailableTrueOrderBySortOrderAsc(
                        List.of(group.getId())))
                .thenReturn(List.of(value));

        PublicCatalogResponse response = service.getBranchCatalog(restaurantId, branchId);

        assertEquals(1, response.menus().size());
        assertEquals(1, response.menus().getFirst().categories().size());
        PublicCatalogItemResponse publicItem = response.menus().getFirst().categories().getFirst().items().getFirst();
        assertEquals(itemId, publicItem.id());
        assertEquals(new BigDecimal("50000"), publicItem.sellingPrice());
        assertEquals(1, publicItem.optionGroups().size());
        assertEquals(1, publicItem.optionGroups().getFirst().values().size());
        verify(restaurantServiceClient).getPublicBranchAvailability(restaurantId, branchId);
    }

    @Test
    void expiredMenuIsExcludedBeforeCategoryQueries() {
        Menu menu = menu();
        menu.setAvailableUntil(LocalDate.now().minusDays(1));
        when(menuRepository.findAllByRestaurantIdAndBranchIdAndStatusOrderByCreatedAtAsc(
                        restaurantId, branchId, CatalogStatus.ACTIVE))
                .thenReturn(List.of(menu));

        PublicCatalogResponse response = service.getBranchCatalog(restaurantId, branchId);

        assertEquals(List.of(), response.menus());
    }

    @Test
    void unavailableBranchItemRemainsVisibleWithEffectiveAvailabilityFalse() {
        CatalogItem item = item();
        BranchItem branchItem = branchItem(item, false, Instant.now().plusSeconds(3600));
        when(itemRepository.findByIdAndRestaurantIdAndStatus(itemId, restaurantId, CatalogStatus.ACTIVE))
                .thenReturn(Optional.of(item));
        when(branchItemRepository.findByBranchIdAndItemId(branchId, itemId))
                .thenReturn(Optional.of(branchItem));
        when(imageRepository.findAllByItemIdOrderByIsPrimaryDescSortOrderAscCreatedAtAsc(itemId))
                .thenReturn(List.of());
        when(optionGroupRepository.findAllByItemIdInAndStatusOrderBySortOrderAsc(
                        List.of(itemId), CatalogStatus.ACTIVE))
                .thenReturn(List.of());

        PublicCatalogItemResponse response = service.getBranchItem(restaurantId, branchId, itemId);

        assertFalse(response.isAvailable());
    }

    @Test
    void itemWithoutBranchItemIsNotExposedAsPublicDetail() {
        when(itemRepository.findByIdAndRestaurantIdAndStatus(itemId, restaurantId, CatalogStatus.ACTIVE))
                .thenReturn(Optional.of(item()));
        when(branchItemRepository.findByBranchIdAndItemId(branchId, itemId))
                .thenReturn(Optional.empty());

        AppException error =
                assertThrows(
                        AppException.class,
                        () -> service.getBranchItem(restaurantId, branchId, itemId));

        assertEquals(ErrorCode.CATALOG_ITEM_NOT_FOUND, error.getErrorCode());
    }

    @Test
    void inactiveRestaurantOrBranchIsHiddenAsNotFound() {
        when(restaurantServiceClient.getPublicBranchAvailability(restaurantId, branchId))
                .thenReturn(
                        ApiResponse.success(
                                "not visible",
                                new RestaurantServiceClient.PublicBranchAvailabilityResponse(
                                        restaurantId, branchId, true, false)));

        AppException error =
                assertThrows(
                        AppException.class,
                        () -> service.getBranchCatalog(restaurantId, branchId));

        assertEquals(ErrorCode.BRANCH_NOT_FOUND, error.getErrorCode());
    }

    private Menu menu() {
        Menu menu = new Menu();
        menu.setId(menuId);
        menu.setRestaurantId(restaurantId);
        menu.setBranchId(branchId);
        menu.setName("Menu");
        menu.setStatus(CatalogStatus.ACTIVE);
        return menu;
    }

    private MenuCategory category(Menu menu) {
        MenuCategory category = new MenuCategory();
        category.setId(categoryId);
        category.setMenu(menu);
        category.setName("Category");
        category.setSortOrder(0);
        category.setStatus(CatalogStatus.ACTIVE);
        return category;
    }

    private CatalogItem item() {
        CatalogItem item = new CatalogItem();
        item.setId(itemId);
        item.setRestaurantId(restaurantId);
        item.setName("Item");
        item.setItemType(CatalogItemType.FOOD);
        item.setBasePrice(new BigDecimal("40000"));
        item.setCurrency("VND");
        item.setStatus(CatalogStatus.ACTIVE);
        return item;
    }

    private MenuCategoryItem mapping(MenuCategory category, CatalogItem item) {
        MenuCategoryItem mapping = new MenuCategoryItem();
        mapping.setCategory(category);
        mapping.setItem(item);
        mapping.setSortOrder(0);
        return mapping;
    }

    private BranchItem branchItem(CatalogItem item, boolean available, Instant soldOutUntil) {
        BranchItem branchItem = new BranchItem();
        branchItem.setBranchId(branchId);
        branchItem.setItem(item);
        branchItem.setSellingPrice(new BigDecimal("50000"));
        branchItem.setIsAvailable(available);
        branchItem.setSoldOutUntil(soldOutUntil);
        return branchItem;
    }

    private OptionGroup optionGroup(CatalogItem item) {
        OptionGroup group = new OptionGroup();
        group.setId(UUID.randomUUID());
        group.setItem(item);
        group.setName("Size");
        group.setSelectionType(OptionSelectionType.SINGLE);
        group.setMinimumSelections(0);
        group.setMaximumSelections(1);
        group.setRequired(false);
        group.setSortOrder(0);
        group.setStatus(CatalogStatus.ACTIVE);
        return group;
    }

    private OptionValue optionValue(OptionGroup group) {
        OptionValue value = new OptionValue();
        value.setId(UUID.randomUUID());
        value.setOptionGroup(group);
        value.setName("M");
        value.setAdditionalPrice(BigDecimal.ZERO);
        value.setSortOrder(0);
        value.setIsAvailable(true);
        return value;
    }
}
