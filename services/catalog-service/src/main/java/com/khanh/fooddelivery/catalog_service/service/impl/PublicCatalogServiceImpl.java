package com.khanh.fooddelivery.catalog_service.service.impl;

import com.khanh.fooddelivery.catalog_service.client.RestaurantServiceClient;
import com.khanh.fooddelivery.catalog_service.common.response.ApiResponse;
import com.khanh.fooddelivery.catalog_service.dto.response.publiccatalog.PublicCatalogItemResponse;
import com.khanh.fooddelivery.catalog_service.dto.response.publiccatalog.PublicCatalogResponse;
import com.khanh.fooddelivery.catalog_service.dto.response.publiccatalog.PublicItemImageResponse;
import com.khanh.fooddelivery.catalog_service.dto.response.publiccatalog.PublicMenuCategoryResponse;
import com.khanh.fooddelivery.catalog_service.dto.response.publiccatalog.PublicMenuResponse;
import com.khanh.fooddelivery.catalog_service.dto.response.publiccatalog.PublicOptionGroupResponse;
import com.khanh.fooddelivery.catalog_service.dto.response.publiccatalog.PublicOptionValueResponse;
import com.khanh.fooddelivery.catalog_service.dto.request.publiccatalog.SellableItemFilterRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.publiccatalog.SellableItemFilterResponse;
import com.khanh.fooddelivery.catalog_service.entity.BranchItem;
import com.khanh.fooddelivery.catalog_service.entity.CatalogItem;
import com.khanh.fooddelivery.catalog_service.entity.ItemImage;
import com.khanh.fooddelivery.catalog_service.entity.Menu;
import com.khanh.fooddelivery.catalog_service.entity.MenuCategory;
import com.khanh.fooddelivery.catalog_service.entity.MenuCategoryItem;
import com.khanh.fooddelivery.catalog_service.entity.OptionGroup;
import com.khanh.fooddelivery.catalog_service.entity.OptionValue;
import com.khanh.fooddelivery.catalog_service.enums.CatalogStatus;
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
import com.khanh.fooddelivery.catalog_service.service.PublicCatalogService;
import com.khanh.fooddelivery.catalog_service.service.CustomerSellabilityService;
import feign.FeignException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PublicCatalogServiceImpl implements PublicCatalogService {
    private final RestaurantServiceClient restaurantServiceClient;
    private final MenuRepository menus;
    private final MenuCategoryRepository categories;
    private final MenuCategoryItemRepository categoryItems;
    private final CatalogItemRepository items;
    private final BranchItemRepository branchItems;
    private final ItemImageRepository images;
    private final OptionGroupRepository optionGroups;
    private final OptionValueRepository optionValues;
    private final CustomerSellabilityService customerSellabilityService;

    @Override
    public PublicCatalogResponse getBranchCatalog(UUID restaurantId, UUID branchId) {
        requirePublicBranch(restaurantId, branchId);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<Menu> visibleMenus =
                menus
                        .findAllByRestaurantIdAndBranchIdAndStatusOrderByCreatedAtAsc(
                                restaurantId, branchId, CatalogStatus.ACTIVE)
                        .stream()
                        .filter(menu -> isAvailableOn(menu, today))
                        .toList();
        if (visibleMenus.isEmpty()) {
            return new PublicCatalogResponse(restaurantId, branchId, List.of());
        }

        List<MenuCategory> visibleCategories =
                categories.findAllByMenuIdInAndStatusOrderBySortOrderAsc(
                        ids(visibleMenus, Menu::getId), CatalogStatus.ACTIVE);
        if (visibleCategories.isEmpty()) {
            return new PublicCatalogResponse(restaurantId, branchId, List.of());
        }

        List<MenuCategoryItem> mappings =
                categoryItems.findAllByCategoryIdInOrderBySortOrderAsc(
                        ids(visibleCategories, MenuCategory::getId));
        PublicItemData itemData =
                loadVisibleItemData(
                        restaurantId,
                        branchId,
                        ids(mappings, mapping -> mapping.getItem().getId()));
        Map<UUID, List<MenuCategoryItem>> mappingsByCategory =
                mappings.stream()
                        .filter(
                                mapping ->
                                        itemData.itemsById().containsKey(mapping.getItem().getId()))
                        .collect(Collectors.groupingBy(mapping -> mapping.getCategory().getId()));
        Map<UUID, List<MenuCategory>> categoriesByMenu =
                visibleCategories.stream()
                        .collect(Collectors.groupingBy(category -> category.getMenu().getId()));

        List<PublicMenuResponse> publicMenus =
                visibleMenus.stream()
                        .map(
                                menu ->
                                        new PublicMenuResponse(
                                                menu.getId(),
                                                menu.getName(),
                                                menu.getDescription(),
                                                categoriesByMenu
                                                        .getOrDefault(menu.getId(), List.of())
                                                        .stream()
                                                        .map(
                                                                category ->
                                                                        toCategoryResponse(
                                                                                category,
                                                                                mappingsByCategory
                                                                                        .getOrDefault(
                                                                                                category
                                                                                                        .getId(),
                                                                                                List
                                                                                                        .of()),
                                                                                itemData))
                                                        .filter(
                                                                category ->
                                                                        !category.items().isEmpty())
                                                        .toList()))
                        .filter(menu -> !menu.categories().isEmpty())
                        .toList();
        return new PublicCatalogResponse(restaurantId, branchId, publicMenus);
    }

    @Override
    public PublicCatalogItemResponse getBranchItem(UUID restaurantId, UUID branchId, UUID itemId) {
        requirePublicBranch(restaurantId, branchId);
        CatalogItem item =
                items.findByIdAndRestaurantIdAndStatus(itemId, restaurantId, CatalogStatus.ACTIVE)
                        .orElseThrow(() -> new AppException(ErrorCode.CATALOG_ITEM_NOT_FOUND));
        if (!customerSellabilityService.isSellable(restaurantId, branchId, itemId)) {
            throw new AppException(ErrorCode.ITEM_NOT_SELLABLE);
        }
        BranchItem branchItem =
                branchItems
                        .findByBranchIdAndItemId(branchId, itemId)
                        .orElseThrow(() -> new AppException(ErrorCode.CATALOG_ITEM_NOT_FOUND));
        return toItemResponse(
                item,
                branchItem,
                images.findAllByItemIdOrderByIsPrimaryDescSortOrderAscCreatedAtAsc(itemId),
                optionGroups.findAllByItemIdInAndStatusOrderBySortOrderAsc(
                        List.of(itemId), CatalogStatus.ACTIVE),
                optionValues);
    }

    @Override
    public SellableItemFilterResponse filterSellableItems(
            UUID branchId, SellableItemFilterRequest request) {
        requirePublicBranch(request.restaurantId(), branchId);
        return new SellableItemFilterResponse(
                customerSellabilityService.filterSellable(
                        request.restaurantId(), branchId, request.itemIds()));
    }

    private PublicItemData loadVisibleItemData(
            UUID restaurantId, UUID branchId, List<UUID> mappedItemIds) {
        if (mappedItemIds.isEmpty()) {
            return PublicItemData.empty();
        }
        Map<UUID, CatalogItem> itemsById =
                items.findAllByIdInAndStatus(mappedItemIds, CatalogStatus.ACTIVE).stream()
                        .filter(item -> item.getRestaurantId().equals(restaurantId))
                        .collect(Collectors.toMap(CatalogItem::getId, Function.identity()));
        if (itemsById.isEmpty()) {
            return PublicItemData.empty();
        }
        Map<UUID, BranchItem> branchItemsByItemId =
                branchItems
                        .findAllByBranchIdAndItemIdIn(branchId, List.copyOf(itemsById.keySet()))
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        branchItem -> branchItem.getItem().getId(),
                                        Function.identity()));
        itemsById.keySet().retainAll(branchItemsByItemId.keySet());
        if (itemsById.isEmpty()) {
            return PublicItemData.empty();
        }
        List<UUID> visibleItemIds = List.copyOf(itemsById.keySet());
        Map<UUID, List<ItemImage>> imagesByItem =
                groupById(
                        images.findAllByItemIdInOrderByIsPrimaryDescSortOrderAscCreatedAtAsc(
                                visibleItemIds),
                        image -> image.getItem().getId());
        List<OptionGroup> activeGroups =
                optionGroups.findAllByItemIdInAndStatusOrderBySortOrderAsc(
                        visibleItemIds, CatalogStatus.ACTIVE);
        Map<UUID, List<OptionGroup>> groupsByItem =
                groupById(activeGroups, group -> group.getItem().getId());
        Map<UUID, List<OptionValue>> valuesByGroup =
                activeGroups.isEmpty()
                        ? Map.of()
                        : groupById(
                                optionValues
                                        .findAllByOptionGroupIdInAndIsAvailableTrueOrderBySortOrderAsc(
                                                ids(activeGroups, OptionGroup::getId)),
                                value -> value.getOptionGroup().getId());
        return new PublicItemData(
                itemsById, branchItemsByItemId, imagesByItem, groupsByItem, valuesByGroup);
    }

    private PublicMenuCategoryResponse toCategoryResponse(
            MenuCategory category, List<MenuCategoryItem> mappings, PublicItemData itemData) {
        return new PublicMenuCategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getSortOrder(),
                mappings.stream()
                        .map(mapping -> mapping.getItem().getId())
                        .map(
                                itemId ->
                                        toItemResponse(
                                                itemData.itemsById().get(itemId),
                                                itemData.branchItemsByItemId().get(itemId),
                                                itemData.imagesByItem()
                                                        .getOrDefault(itemId, List.of()),
                                                itemData.groupsByItem()
                                                        .getOrDefault(itemId, List.of()),
                                                itemData.valuesByGroup()))
                        .toList());
    }

    private PublicCatalogItemResponse toItemResponse(
            CatalogItem item,
            BranchItem branchItem,
            List<ItemImage> itemImages,
            List<OptionGroup> groups,
            OptionValueRepository valuesRepository) {
        Map<UUID, List<OptionValue>> valuesByGroup =
                groups.isEmpty()
                        ? Map.of()
                        : groupById(
                                valuesRepository
                                        .findAllByOptionGroupIdInAndIsAvailableTrueOrderBySortOrderAsc(
                                                ids(groups, OptionGroup::getId)),
                                value -> value.getOptionGroup().getId());
        return toItemResponse(item, branchItem, itemImages, groups, valuesByGroup);
    }

    private PublicCatalogItemResponse toItemResponse(
            CatalogItem item,
            BranchItem branchItem,
            List<ItemImage> itemImages,
            List<OptionGroup> groups,
            Map<UUID, List<OptionValue>> valuesByGroup) {
        List<PublicItemImageResponse> publicImages =
                itemImages.stream()
                        .map(
                                image ->
                                        new PublicItemImageResponse(
                                                image.getId(),
                                                image.getImageUrl(),
                                                image.getSortOrder(),
                                                image.getIsPrimary()))
                        .toList();
        String primaryImageUrl =
                itemImages.stream()
                        .filter(image -> Boolean.TRUE.equals(image.getIsPrimary()))
                        .map(ItemImage::getImageUrl)
                        .findFirst()
                        .orElse(null);
        List<PublicOptionGroupResponse> publicGroups =
                groups.stream()
                        .map(
                                group ->
                                        new PublicOptionGroupResponse(
                                                group.getId(),
                                                group.getName(),
                                                group.getSelectionType(),
                                                group.getMinimumSelections(),
                                                group.getMaximumSelections(),
                                                group.getRequired(),
                                                group.getSortOrder(),
                                                valuesByGroup
                                                        .getOrDefault(group.getId(), List.of())
                                                        .stream()
                                                        .map(
                                                                value ->
                                                                        new PublicOptionValueResponse(
                                                                                value.getId(),
                                                                                value.getName(),
                                                                                value
                                                                                        .getAdditionalPrice(),
                                                                                value
                                                                                        .getSortOrder()))
                                                        .toList()))
                        .toList();
        return new PublicCatalogItemResponse(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getItemType(),
                branchItem.getSellingPrice(),
                branchItem.getOriginalPrice(),
                item.getCurrency(),
                isEffectivelyAvailable(branchItem),
                branchItem.getAvailableQuantity(),
                branchItem.getSoldOutUntil(),
                item.getPreparationTimeMinutes(),
                item.getIsVegetarian(),
                primaryImageUrl,
                publicImages,
                publicGroups);
    }

    private void requirePublicBranch(UUID restaurantId, UUID branchId) {
        try {
            ApiResponse<RestaurantServiceClient.PublicBranchAvailabilityResponse> response =
                    restaurantServiceClient.getPublicBranchAvailability(restaurantId, branchId);
            if (!response.success() || response.data() == null) {
                throw new AppException(ErrorCode.RESTAURANT_SERVICE_UNAVAILABLE);
            }
            if (!response.data().restaurantVisible() || !response.data().branchVisible()) {
                throw new AppException(ErrorCode.BRANCH_NOT_FOUND);
            }
        } catch (FeignException.NotFound exception) {
            throw new AppException(ErrorCode.BRANCH_NOT_FOUND);
        } catch (FeignException exception) {
            throw new AppException(ErrorCode.RESTAURANT_SERVICE_UNAVAILABLE);
        }
    }

    private boolean isAvailableOn(Menu menu, LocalDate today) {
        return (menu.getAvailableFrom() == null || !menu.getAvailableFrom().isAfter(today))
                && (menu.getAvailableUntil() == null || !menu.getAvailableUntil().isBefore(today));
    }

    private boolean isEffectivelyAvailable(BranchItem branchItem) {
        return Boolean.TRUE.equals(branchItem.getIsAvailable())
                && (branchItem.getSoldOutUntil() == null
                        || !branchItem.getSoldOutUntil().isAfter(Instant.now()));
    }

    private <T> List<UUID> ids(List<T> entities, Function<T, UUID> idExtractor) {
        return entities.stream().map(idExtractor).distinct().toList();
    }

    private <T> Map<UUID, List<T>> groupById(List<T> entities, Function<T, UUID> idExtractor) {
        return entities.stream().collect(Collectors.groupingBy(idExtractor));
    }

    private record PublicItemData(
            Map<UUID, CatalogItem> itemsById,
            Map<UUID, BranchItem> branchItemsByItemId,
            Map<UUID, List<ItemImage>> imagesByItem,
            Map<UUID, List<OptionGroup>> groupsByItem,
            Map<UUID, List<OptionValue>> valuesByGroup) {
        private static PublicItemData empty() {
            return new PublicItemData(Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
        }
    }
}
