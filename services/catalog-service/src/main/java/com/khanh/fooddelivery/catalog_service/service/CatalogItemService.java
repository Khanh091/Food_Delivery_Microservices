package com.khanh.fooddelivery.catalog_service.service;

import com.khanh.fooddelivery.catalog_service.dto.request.CatalogItemCreateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.CatalogItemUpdateRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.CatalogItemLibraryPageResponse;
import com.khanh.fooddelivery.catalog_service.dto.response.CatalogItemResponse;
import java.util.List;
import java.util.UUID;

public interface CatalogItemService {
    CatalogItemResponse create(CatalogItemCreateRequest request);

    CatalogItemResponse get(UUID itemId);

    List<CatalogItemResponse> list(UUID restaurantId);

    CatalogItemLibraryPageResponse listLibrary(
            UUID restaurantId, String query, List<UUID> excludedItemIds, int page, int size);

    CatalogItemResponse update(UUID itemId, CatalogItemUpdateRequest request);

    CatalogItemResponse activate(UUID itemId);

    CatalogItemResponse deactivate(UUID itemId);
}
