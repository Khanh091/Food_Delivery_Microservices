package com.khanh.fooddelivery.catalog_service.controller;

import com.khanh.fooddelivery.catalog_service.common.response.ApiResponse;
import com.khanh.fooddelivery.catalog_service.dto.request.MenuCreateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.MenuUpdateRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.MenuResponse;
import com.khanh.fooddelivery.catalog_service.service.MenuService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog/menus")
@RequiredArgsConstructor
public class MenuController {
    private final MenuService menuService;

    @PostMapping
    public ResponseEntity<ApiResponse<MenuResponse>> create(
            @Valid @RequestBody MenuCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Menu created successfully", menuService.create(request)));
    }

    @GetMapping("/{menuId}")
    public ApiResponse<MenuResponse> get(@PathVariable UUID menuId) {
        return ApiResponse.success("Menu retrieved successfully", menuService.get(menuId));
    }

    @GetMapping
    public ApiResponse<List<MenuResponse>> list(
            @RequestParam UUID restaurantId, @RequestParam UUID branchId) {
        return ApiResponse.success(
                "Menus retrieved successfully", menuService.list(restaurantId, branchId));
    }

    @PatchMapping("/{menuId}")
    public ApiResponse<MenuResponse> update(
            @PathVariable UUID menuId, @Valid @RequestBody MenuUpdateRequest request) {
        return ApiResponse.success(
                "Menu updated successfully", menuService.update(menuId, request));
    }

    @PatchMapping("/{menuId}/activate")
    public ApiResponse<MenuResponse> activate(@PathVariable UUID menuId) {
        return ApiResponse.success("Menu activated", menuService.activate(menuId));
    }

    @PatchMapping("/{menuId}/deactivate")
    public ApiResponse<MenuResponse> deactivate(@PathVariable UUID menuId) {
        return ApiResponse.success("Menu deactivated", menuService.deactivate(menuId));
    }

    @DeleteMapping("/{menuId}")
    public ApiResponse<Void> delete(@PathVariable UUID menuId) {
        menuService.delete(menuId);
        return ApiResponse.success("Menu deleted successfully");
    }
}
