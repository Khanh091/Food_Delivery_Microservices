package com.khanh.fooddelivery.catalog_service.controller;

import com.khanh.fooddelivery.catalog_service.common.response.ApiResponse;
import com.khanh.fooddelivery.catalog_service.dto.request.MenuCategoryItemCreateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.MenuCategoryItemBatchCreateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.MenuCategoryItemSortOrderUpdateRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.MenuCategoryItemResponse;
import com.khanh.fooddelivery.catalog_service.service.MenuCategoryItemService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog/menus/{menuId}/categories/{categoryId}/items")
@RequiredArgsConstructor
public class MenuCategoryItemController {
    private final MenuCategoryItemService categoryItemService;

    @PostMapping("/{itemId}")
    public ResponseEntity<ApiResponse<MenuCategoryItemResponse>> add(
            @PathVariable UUID menuId,
            @PathVariable UUID categoryId,
            @PathVariable UUID itemId,
            @Valid @RequestBody MenuCategoryItemCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Catalog item added to category",
                                categoryItemService.add(menuId, categoryId, itemId, request)));
    }

    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<List<MenuCategoryItemResponse>>> addBatch(
            @PathVariable UUID menuId,
            @PathVariable UUID categoryId,
            @Valid @RequestBody MenuCategoryItemBatchCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Catalog items added to category", categoryItemService.addBatch(menuId, categoryId, request)));
    }

    @GetMapping
    public ApiResponse<List<MenuCategoryItemResponse>> list(
            @PathVariable UUID menuId, @PathVariable UUID categoryId) {
        return ApiResponse.success(
                "Category items retrieved successfully",
                categoryItemService.list(menuId, categoryId));
    }

    @PatchMapping("/{itemId}")
    public ApiResponse<MenuCategoryItemResponse> updateSortOrder(
            @PathVariable UUID menuId,
            @PathVariable UUID categoryId,
            @PathVariable UUID itemId,
            @Valid @RequestBody MenuCategoryItemSortOrderUpdateRequest request) {
        return ApiResponse.success(
                "Category item sort order updated",
                categoryItemService.updateSortOrder(menuId, categoryId, itemId, request));
    }

    @DeleteMapping("/{itemId}")
    public ApiResponse<Void> remove(
            @PathVariable UUID menuId, @PathVariable UUID categoryId, @PathVariable UUID itemId) {
        categoryItemService.remove(menuId, categoryId, itemId);
        return ApiResponse.success("Catalog item removed from category");
    }
}
