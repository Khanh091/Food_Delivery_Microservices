package com.khanh.fooddelivery.catalog_service.service.impl;

import com.khanh.fooddelivery.catalog_service.entity.BranchItem;
import com.khanh.fooddelivery.catalog_service.entity.CatalogItem;
import com.khanh.fooddelivery.catalog_service.entity.Menu;
import com.khanh.fooddelivery.catalog_service.entity.MenuCategory;
import com.khanh.fooddelivery.catalog_service.entity.MenuCategoryItem;
import com.khanh.fooddelivery.catalog_service.enums.CatalogStatus;
import com.khanh.fooddelivery.catalog_service.repository.BranchItemRepository;
import com.khanh.fooddelivery.catalog_service.repository.CatalogItemRepository;
import com.khanh.fooddelivery.catalog_service.repository.MenuCategoryItemRepository;
import com.khanh.fooddelivery.catalog_service.repository.MenuCategoryRepository;
import com.khanh.fooddelivery.catalog_service.repository.MenuRepository;
import com.khanh.fooddelivery.catalog_service.service.CustomerSellabilityService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerSellabilityServiceImpl implements CustomerSellabilityService {
    private final MenuRepository menus;
    private final MenuCategoryRepository categories;
    private final MenuCategoryItemRepository categoryItems;
    private final CatalogItemRepository items;
    private final BranchItemRepository branchItems;

    @Override
    public boolean isSellable(UUID restaurantId, UUID branchId, UUID itemId) {
        return filterSellable(restaurantId, branchId, List.of(itemId)).contains(itemId);
    }

    @Override
    public List<UUID> filterSellable(UUID restaurantId, UUID branchId, List<UUID> itemIds) {
        LinkedHashSet<UUID> requestedItemIds = new LinkedHashSet<>();
        if (itemIds != null) {
            itemIds.stream().filter(java.util.Objects::nonNull).forEach(requestedItemIds::add);
        }
        if (requestedItemIds.isEmpty()) {
            return List.of();
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<Menu> activeMenus =
                menus.findAllByRestaurantIdAndBranchIdAndStatusOrderByCreatedAtAsc(
                                restaurantId, branchId, CatalogStatus.ACTIVE)
                        .stream()
                        .filter(menu -> isEffectiveOn(menu, today))
                        .toList();
        if (activeMenus.isEmpty()) {
            return List.of();
        }

        List<MenuCategory> activeCategories =
                categories.findAllByMenuIdInAndStatusOrderBySortOrderAsc(
                        ids(activeMenus, Menu::getId), CatalogStatus.ACTIVE);
        if (activeCategories.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<UUID> placedItemIds =
                categoryItems
                        .findAllByCategoryIdInOrderBySortOrderAsc(ids(activeCategories, MenuCategory::getId))
                        .stream()
                        .map(mapping -> mapping.getItem().getId())
                        .filter(requestedItemIds::contains)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        if (placedItemIds.isEmpty()) {
            return List.of();
        }

        Map<UUID, CatalogItem> activeItems =
                items.findAllByIdInAndStatus(List.copyOf(placedItemIds), CatalogStatus.ACTIVE).stream()
                        .filter(item -> restaurantId.equals(item.getRestaurantId()))
                        .collect(Collectors.toMap(CatalogItem::getId, Function.identity()));
        if (activeItems.isEmpty()) {
            return List.of();
        }

        Map<UUID, BranchItem> branchItemsByItemId =
                branchItems.findAllByBranchIdAndItemIdIn(branchId, List.copyOf(activeItems.keySet())).stream()
                        .collect(
                                Collectors.toMap(
                                        branchItem -> branchItem.getItem().getId(),
                                        Function.identity()));

        return requestedItemIds.stream()
                .filter(activeItems::containsKey)
                .filter(itemId -> isEffectivelyAvailable(branchItemsByItemId.get(itemId)))
                .toList();
    }

    private boolean isEffectiveOn(Menu menu, LocalDate date) {
        return (menu.getAvailableFrom() == null || !date.isBefore(menu.getAvailableFrom()))
                && (menu.getAvailableUntil() == null || !date.isAfter(menu.getAvailableUntil()));
    }

    private boolean isEffectivelyAvailable(BranchItem branchItem) {
        return branchItem != null
                && Boolean.TRUE.equals(branchItem.getIsAvailable())
                && (branchItem.getSoldOutUntil() == null
                        || !branchItem.getSoldOutUntil().isAfter(Instant.now()));
    }

    private <T> List<UUID> ids(Collection<T> source, Function<T, UUID> idExtractor) {
        return source.stream().map(idExtractor).toList();
    }
}
