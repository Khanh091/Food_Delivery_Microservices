package com.khanh.fooddelivery.catalog_service.service;

import com.khanh.fooddelivery.catalog_service.dto.request.MenuCategoryItemCreateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.MenuCategoryItemBatchCreateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.MenuCategoryItemSortOrderUpdateRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.MenuCategoryItemResponse;
import java.util.List;
import java.util.UUID;

public interface MenuCategoryItemService {
    MenuCategoryItemResponse add(
            UUID menuId, UUID categoryId, UUID itemId, MenuCategoryItemCreateRequest request);

    List<MenuCategoryItemResponse> addBatch(
            UUID menuId, UUID categoryId, MenuCategoryItemBatchCreateRequest request);

    List<MenuCategoryItemResponse> list(UUID menuId, UUID categoryId);

    MenuCategoryItemResponse updateSortOrder(
            UUID menuId,
            UUID categoryId,
            UUID itemId,
            MenuCategoryItemSortOrderUpdateRequest request);

    void remove(UUID menuId, UUID categoryId, UUID itemId);
}
