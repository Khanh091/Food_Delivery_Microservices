package com.khanh.fooddelivery.catalog_service.service;

import java.util.UUID;

public interface CatalogAuthorizationService {
    void requireRestaurantCatalogAccess(UUID restaurantId);

    void requireBranchCatalogAccess(UUID restaurantId, UUID branchId);
}
