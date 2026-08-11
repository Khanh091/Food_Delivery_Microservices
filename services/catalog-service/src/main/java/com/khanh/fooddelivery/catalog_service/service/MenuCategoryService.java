package com.khanh.fooddelivery.catalog_service.service;

import com.khanh.fooddelivery.catalog_service.dto.request.MenuCategoryCreateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.MenuCategoryUpdateRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.MenuCategoryResponse;
import java.util.List;
import java.util.UUID;

public interface MenuCategoryService {
    MenuCategoryResponse create(UUID menuId, MenuCategoryCreateRequest request);

    MenuCategoryResponse get(UUID menuId, UUID categoryId);

    List<MenuCategoryResponse> list(UUID menuId);

    MenuCategoryResponse update(UUID menuId, UUID categoryId, MenuCategoryUpdateRequest request);

    MenuCategoryResponse activate(UUID menuId, UUID categoryId);

    MenuCategoryResponse deactivate(UUID menuId, UUID categoryId);

    void delete(UUID menuId, UUID categoryId);
}
