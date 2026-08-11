package com.khanh.fooddelivery.catalog_service.service;

import com.khanh.fooddelivery.catalog_service.dto.request.MenuCreateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.MenuUpdateRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.MenuResponse;
import java.util.List;
import java.util.UUID;

public interface MenuService {
    MenuResponse create(MenuCreateRequest request);

    MenuResponse get(UUID menuId);

    List<MenuResponse> list(UUID restaurantId, UUID branchId);

    MenuResponse update(UUID menuId, MenuUpdateRequest request);

    MenuResponse activate(UUID menuId);

    MenuResponse deactivate(UUID menuId);

    void delete(UUID menuId);
}
