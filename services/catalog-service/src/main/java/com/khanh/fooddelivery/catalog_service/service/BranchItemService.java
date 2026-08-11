package com.khanh.fooddelivery.catalog_service.service;

import com.khanh.fooddelivery.catalog_service.dto.request.BranchItemCreateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.BranchItemPriceUpdateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.BranchItemQuantityUpdateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.BranchItemSoldOutRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.BranchItemResponse;
import com.khanh.fooddelivery.catalog_service.dto.response.ItemPriceHistoryResponse;
import java.util.List;
import java.util.UUID;

public interface BranchItemService {
    BranchItemResponse create(BranchItemCreateRequest request);

    BranchItemResponse get(UUID branchItemId);

    List<BranchItemResponse> listByBranch(UUID restaurantId, UUID branchId);

    BranchItemResponse updatePrice(UUID branchItemId, BranchItemPriceUpdateRequest request);

    BranchItemResponse markAvailable(UUID branchItemId);

    BranchItemResponse markUnavailable(UUID branchItemId);

    BranchItemResponse markSoldOut(UUID branchItemId, BranchItemSoldOutRequest request);

    BranchItemResponse updateQuantity(UUID branchItemId, BranchItemQuantityUpdateRequest request);

    List<ItemPriceHistoryResponse> getPriceHistory(UUID branchItemId);
}
