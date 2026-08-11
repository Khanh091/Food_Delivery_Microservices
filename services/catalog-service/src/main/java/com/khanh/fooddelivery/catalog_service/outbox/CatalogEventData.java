package com.khanh.fooddelivery.catalog_service.outbox;

import com.khanh.fooddelivery.catalog_service.entity.BranchItem;
import com.khanh.fooddelivery.catalog_service.entity.CatalogItem;
import com.khanh.fooddelivery.catalog_service.entity.Menu;
import com.khanh.fooddelivery.catalog_service.entity.MenuCategory;
import com.khanh.fooddelivery.catalog_service.entity.MenuCategoryItem;
import com.khanh.fooddelivery.catalog_service.entity.OptionGroup;
import com.khanh.fooddelivery.catalog_service.entity.OptionValue;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CatalogEventData {
    private CatalogEventData() {}

    public static Map<String, Object> catalogItem(CatalogItem item, String action) {
        Map<String, Object> data = base(action);
        data.put("itemId", item.getId());
        data.put("restaurantId", item.getRestaurantId());
        data.put("name", item.getName());
        data.put("description", item.getDescription());
        data.put("itemType", item.getItemType());
        data.put("basePrice", item.getBasePrice());
        data.put("currency", item.getCurrency());
        data.put("preparationTimeMinutes", item.getPreparationTimeMinutes());
        data.put("isVegetarian", item.getIsVegetarian());
        data.put("status", item.getStatus());
        return data;
    }

    public static Map<String, Object> branchItem(BranchItem item, String action) {
        Map<String, Object> data = base(action);
        data.put("branchItemId", item.getId());
        data.put("itemId", item.getItem().getId());
        data.put("restaurantId", item.getItem().getRestaurantId());
        data.put("branchId", item.getBranchId());
        data.put("sellingPrice", item.getSellingPrice());
        data.put("originalPrice", item.getOriginalPrice());
        data.put("isAvailable", item.getIsAvailable());
        data.put("availableQuantity", item.getAvailableQuantity());
        data.put("soldOutUntil", item.getSoldOutUntil());
        return data;
    }

    public static Map<String, Object> menu(Menu menu, String action) {
        Map<String, Object> data = base(action);
        data.put("menuId", menu.getId());
        data.put("restaurantId", menu.getRestaurantId());
        data.put("branchId", menu.getBranchId());
        data.put("name", menu.getName());
        data.put("description", menu.getDescription());
        data.put("status", menu.getStatus());
        data.put("availableFrom", menu.getAvailableFrom());
        data.put("availableUntil", menu.getAvailableUntil());
        return data;
    }

    public static Map<String, Object> menuCategory(MenuCategory category, String action) {
        Map<String, Object> data = base(action);
        data.put("categoryId", category.getId());
        data.put("menuId", category.getMenu().getId());
        data.put("restaurantId", category.getMenu().getRestaurantId());
        data.put("branchId", category.getMenu().getBranchId());
        data.put("name", category.getName());
        data.put("description", category.getDescription());
        data.put("sortOrder", category.getSortOrder());
        data.put("status", category.getStatus());
        return data;
    }

    public static Map<String, Object> menuCategoryItem(MenuCategoryItem mapping, String action) {
        Map<String, Object> data = base(action);
        data.put("mappingId", mapping.getId());
        data.put("categoryId", mapping.getCategory().getId());
        data.put("menuId", mapping.getCategory().getMenu().getId());
        data.put("itemId", mapping.getItem().getId());
        data.put("restaurantId", mapping.getItem().getRestaurantId());
        data.put("branchId", mapping.getCategory().getMenu().getBranchId());
        data.put("sortOrder", mapping.getSortOrder());
        return data;
    }

    public static Map<String, Object> optionGroup(OptionGroup group, String action) {
        Map<String, Object> data = base(action);
        data.put("optionGroupId", group.getId());
        data.put("itemId", group.getItem().getId());
        data.put("restaurantId", group.getItem().getRestaurantId());
        data.put("name", group.getName());
        data.put("selectionType", group.getSelectionType());
        data.put("minimumSelections", group.getMinimumSelections());
        data.put("maximumSelections", group.getMaximumSelections());
        data.put("required", group.getRequired());
        data.put("status", group.getStatus());
        data.put("sortOrder", group.getSortOrder());
        return data;
    }

    public static Map<String, Object> optionValue(OptionValue value, String action) {
        Map<String, Object> data = base(action);
        data.put("optionValueId", value.getId());
        data.put("optionGroupId", value.getOptionGroup().getId());
        data.put("itemId", value.getOptionGroup().getItem().getId());
        data.put("restaurantId", value.getOptionGroup().getItem().getRestaurantId());
        data.put("name", value.getName());
        data.put("additionalPrice", value.getAdditionalPrice());
        data.put("isAvailable", value.getIsAvailable());
        data.put("sortOrder", value.getSortOrder());
        return data;
    }

    private static Map<String, Object> base(String action) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("action", action);
        return data;
    }
}
