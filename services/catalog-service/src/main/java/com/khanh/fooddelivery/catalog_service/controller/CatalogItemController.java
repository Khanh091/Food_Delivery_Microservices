package com.khanh.fooddelivery.catalog_service.controller;

import com.khanh.fooddelivery.catalog_service.common.response.ApiResponse;
import com.khanh.fooddelivery.catalog_service.dto.request.CatalogItemCreateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.CatalogItemUpdateRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.CatalogItemResponse;
import com.khanh.fooddelivery.catalog_service.service.CatalogItemService;
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
@RequestMapping("/api/v1/catalog/items")
@RequiredArgsConstructor
public class CatalogItemController {
    private final CatalogItemService itemService;

    @PostMapping
    public ResponseEntity<ApiResponse<CatalogItemResponse>> create(
            @Valid @RequestBody CatalogItemCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Catalog item created successfully", itemService.create(request)));
    }

    @GetMapping("/{itemId}")
    public ApiResponse<CatalogItemResponse> get(@PathVariable UUID itemId) {
        return ApiResponse.success("Catalog item retrieved successfully", itemService.get(itemId));
    }

    @GetMapping
    public ApiResponse<List<CatalogItemResponse>> list(@RequestParam UUID restaurantId) {
        return ApiResponse.success(
                "Catalog items retrieved successfully", itemService.list(restaurantId));
    }

    @PatchMapping("/{itemId}")
    public ApiResponse<CatalogItemResponse> update(
            @PathVariable UUID itemId, @Valid @RequestBody CatalogItemUpdateRequest request) {
        return ApiResponse.success(
                "Catalog item updated successfully", itemService.update(itemId, request));
    }

    @PatchMapping("/{itemId}/activate")
    public ApiResponse<CatalogItemResponse> activate(@PathVariable UUID itemId) {
        return ApiResponse.success("Catalog item activated", itemService.activate(itemId));
    }

    @PatchMapping("/{itemId}/deactivate")
    public ApiResponse<CatalogItemResponse> deactivate(@PathVariable UUID itemId) {
        return ApiResponse.success("Catalog item deactivated", itemService.deactivate(itemId));
    }
}
