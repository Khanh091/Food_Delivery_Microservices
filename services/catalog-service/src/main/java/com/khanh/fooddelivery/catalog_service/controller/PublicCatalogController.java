package com.khanh.fooddelivery.catalog_service.controller;

import com.khanh.fooddelivery.catalog_service.common.response.ApiResponse;
import com.khanh.fooddelivery.catalog_service.dto.response.publiccatalog.PublicCatalogItemResponse;
import com.khanh.fooddelivery.catalog_service.dto.response.publiccatalog.PublicCatalogResponse;
import com.khanh.fooddelivery.catalog_service.service.PublicCatalogService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/catalog/restaurants/{restaurantId}/branches/{branchId}")
@RequiredArgsConstructor
public class PublicCatalogController {
    private final PublicCatalogService publicCatalogService;

    @GetMapping
    public ApiResponse<PublicCatalogResponse> getBranchCatalog(
            @PathVariable UUID restaurantId, @PathVariable UUID branchId) {
        return ApiResponse.success(
                "Public branch catalog retrieved",
                publicCatalogService.getBranchCatalog(restaurantId, branchId));
    }

    @GetMapping("/items/{itemId}")
    public ApiResponse<PublicCatalogItemResponse> getBranchItem(
            @PathVariable UUID restaurantId,
            @PathVariable UUID branchId,
            @PathVariable UUID itemId) {
        return ApiResponse.success(
                "Public catalog item retrieved",
                publicCatalogService.getBranchItem(restaurantId, branchId, itemId));
    }
}
