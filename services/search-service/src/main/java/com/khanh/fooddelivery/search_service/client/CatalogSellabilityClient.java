package com.khanh.fooddelivery.search_service.client;

import com.khanh.fooddelivery.search_service.common.response.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "catalog-service", contextId = "catalogSellabilityClient")
public interface CatalogSellabilityClient {
    @PostMapping("/api/v1/public/catalog/restaurants/{restaurantId}/branches/{branchId}/sellable-items")
    ApiResponse<SellableItemFilterResponse> filterSellableItems(
            @PathVariable UUID restaurantId,
            @PathVariable UUID branchId,
            @RequestBody SellableItemFilterRequest request);

    record SellableItemFilterRequest(UUID restaurantId, List<UUID> itemIds) {}

    record SellableItemFilterResponse(List<UUID> itemIds) {}
}
