package com.khanh.fooddelivery.catalog_service.controller;

import com.khanh.fooddelivery.catalog_service.common.response.ApiResponse;
import com.khanh.fooddelivery.catalog_service.dto.request.BranchItemCreateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.BranchItemPriceUpdateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.BranchItemQuantityUpdateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.BranchItemSoldOutRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.BranchItemResponse;
import com.khanh.fooddelivery.catalog_service.dto.response.ItemPriceHistoryResponse;
import com.khanh.fooddelivery.catalog_service.service.BranchItemService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog/branch-items")
@RequiredArgsConstructor
public class BranchItemController {
    private final BranchItemService branchItemService;

    @PostMapping
    public ResponseEntity<ApiResponse<BranchItemResponse>> create(
            @Valid @RequestBody BranchItemCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Branch item created successfully",
                                branchItemService.create(request)));
    }

    @GetMapping("/{branchItemId}")
    public ApiResponse<BranchItemResponse> get(@PathVariable UUID branchItemId) {
        return ApiResponse.success(
                "Branch item retrieved successfully", branchItemService.get(branchItemId));
    }

    @GetMapping
    public ApiResponse<List<BranchItemResponse>> list(
            @RequestParam UUID restaurantId, @RequestParam UUID branchId) {
        return ApiResponse.success(
                "Branch items retrieved successfully",
                branchItemService.listByBranch(restaurantId, branchId));
    }

    @PatchMapping("/{branchItemId}/price")
    public ApiResponse<BranchItemResponse> updatePrice(
            @PathVariable UUID branchItemId,
            @Valid @RequestBody BranchItemPriceUpdateRequest request) {
        return ApiResponse.success(
                "Branch item price updated", branchItemService.updatePrice(branchItemId, request));
    }

    @PatchMapping("/{branchItemId}/available")
    public ApiResponse<BranchItemResponse> markAvailable(@PathVariable UUID branchItemId) {
        return ApiResponse.success(
                "Branch item marked available", branchItemService.markAvailable(branchItemId));
    }

    @PatchMapping("/{branchItemId}/unavailable")
    public ApiResponse<BranchItemResponse> markUnavailable(@PathVariable UUID branchItemId) {
        return ApiResponse.success(
                "Branch item marked unavailable", branchItemService.markUnavailable(branchItemId));
    }

    @PatchMapping("/{branchItemId}/sold-out")
    public ApiResponse<BranchItemResponse> markSoldOut(
            @PathVariable UUID branchItemId, @Valid @RequestBody BranchItemSoldOutRequest request) {
        return ApiResponse.success(
                "Branch item marked sold out",
                branchItemService.markSoldOut(branchItemId, request));
    }

    @PatchMapping("/{branchItemId}/quantity")
    public ApiResponse<BranchItemResponse> updateQuantity(
            @PathVariable UUID branchItemId,
            @Valid @RequestBody BranchItemQuantityUpdateRequest request) {
        return ApiResponse.success(
                "Branch item quantity updated",
                branchItemService.updateQuantity(branchItemId, request));
    }

    @GetMapping("/{branchItemId}/price-history")
    public ApiResponse<List<ItemPriceHistoryResponse>> getPriceHistory(
            @PathVariable UUID branchItemId) {
        return ApiResponse.success(
                "Branch item price history retrieved",
                branchItemService.getPriceHistory(branchItemId));
    }
}
