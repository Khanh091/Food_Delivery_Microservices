package com.khanh.fooddelivery.catalog_service.controller;

import com.khanh.fooddelivery.catalog_service.common.response.ApiResponse;
import com.khanh.fooddelivery.catalog_service.dto.request.MenuCategoryCreateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.MenuCategoryUpdateRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.MenuCategoryResponse;
import com.khanh.fooddelivery.catalog_service.service.MenuCategoryService;
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
@RequestMapping("/api/v1/catalog/menus/{menuId}/categories")
@RequiredArgsConstructor
public class MenuCategoryController {
    private final MenuCategoryService categoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<MenuCategoryResponse>> create(
            @PathVariable UUID menuId, @Valid @RequestBody MenuCategoryCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Menu category created successfully",
                                categoryService.create(menuId, request)));
    }

    @GetMapping
    public ApiResponse<List<MenuCategoryResponse>> list(@PathVariable UUID menuId) {
        return ApiResponse.success(
                "Menu categories retrieved successfully", categoryService.list(menuId));
    }

    @GetMapping("/{categoryId}")
    public ApiResponse<MenuCategoryResponse> get(
            @PathVariable UUID menuId, @PathVariable UUID categoryId) {
        return ApiResponse.success(
                "Menu category retrieved successfully", categoryService.get(menuId, categoryId));
    }

    @PatchMapping("/{categoryId}")
    public ApiResponse<MenuCategoryResponse> update(
            @PathVariable UUID menuId,
            @PathVariable UUID categoryId,
            @Valid @RequestBody MenuCategoryUpdateRequest request) {
        return ApiResponse.success(
                "Menu category updated successfully",
                categoryService.update(menuId, categoryId, request));
    }

    @PatchMapping("/{categoryId}/activate")
    public ApiResponse<MenuCategoryResponse> activate(
            @PathVariable UUID menuId, @PathVariable UUID categoryId) {
        return ApiResponse.success(
                "Menu category activated", categoryService.activate(menuId, categoryId));
    }

    @PatchMapping("/{categoryId}/deactivate")
    public ApiResponse<MenuCategoryResponse> deactivate(
            @PathVariable UUID menuId, @PathVariable UUID categoryId) {
        return ApiResponse.success(
                "Menu category deactivated", categoryService.deactivate(menuId, categoryId));
    }

    @DeleteMapping("/{categoryId}")
    public ApiResponse<Void> delete(@PathVariable UUID menuId, @PathVariable UUID categoryId) {
        categoryService.delete(menuId, categoryId);
        return ApiResponse.success("Menu category deleted successfully");
    }
}
