package com.khanh.fooddelivery.catalog_service.service;

import com.khanh.fooddelivery.catalog_service.dto.response.publiccatalog.PublicCatalogItemResponse;
import com.khanh.fooddelivery.catalog_service.dto.response.publiccatalog.PublicCatalogResponse;
import com.khanh.fooddelivery.catalog_service.dto.response.publiccatalog.SellableItemFilterResponse;
import com.khanh.fooddelivery.catalog_service.dto.request.publiccatalog.SellableItemFilterRequest;
import java.util.UUID;

public interface PublicCatalogService {
    PublicCatalogResponse getBranchCatalog(UUID restaurantId, UUID branchId);

    PublicCatalogItemResponse getBranchItem(UUID restaurantId, UUID branchId, UUID itemId);

    SellableItemFilterResponse filterSellableItems(
            UUID branchId, SellableItemFilterRequest request);
}
