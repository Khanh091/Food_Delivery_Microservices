package com.khanh.fooddelivery.catalog_service.service;

import java.util.List;
import java.util.UUID;

public interface CustomerSellabilityService {
    boolean isSellable(UUID restaurantId, UUID branchId, UUID itemId);

    List<UUID> filterSellable(UUID restaurantId, UUID branchId, List<UUID> itemIds);
}
