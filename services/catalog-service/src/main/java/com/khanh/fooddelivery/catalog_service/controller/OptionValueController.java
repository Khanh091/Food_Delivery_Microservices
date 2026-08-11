package com.khanh.fooddelivery.catalog_service.controller;

import com.khanh.fooddelivery.catalog_service.common.response.ApiResponse;
import com.khanh.fooddelivery.catalog_service.dto.request.OptionValueCreateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.OptionValueUpdateRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.OptionValueResponse;
import com.khanh.fooddelivery.catalog_service.service.OptionValueService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/catalog/items/{itemId}/option-groups/{optionGroupId}/values")
@RequiredArgsConstructor
public class OptionValueController {
    private final OptionValueService valueService;

    @PostMapping
    public ResponseEntity<ApiResponse<OptionValueResponse>> create(
            @PathVariable UUID itemId,
            @PathVariable UUID optionGroupId,
            @Valid @RequestBody OptionValueCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Option value created successfully",
                                valueService.create(itemId, optionGroupId, request)));
    }

    @GetMapping
    public ApiResponse<List<OptionValueResponse>> list(
            @PathVariable UUID itemId, @PathVariable UUID optionGroupId) {
        return ApiResponse.success(
                "Option values retrieved successfully", valueService.list(itemId, optionGroupId));
    }

    @GetMapping("/{optionValueId}")
    public ApiResponse<OptionValueResponse> get(
            @PathVariable UUID itemId,
            @PathVariable UUID optionGroupId,
            @PathVariable UUID optionValueId) {
        return ApiResponse.success(
                "Option value retrieved successfully",
                valueService.get(itemId, optionGroupId, optionValueId));
    }

    @PatchMapping("/{optionValueId}")
    public ApiResponse<OptionValueResponse> update(
            @PathVariable UUID itemId,
            @PathVariable UUID optionGroupId,
            @PathVariable UUID optionValueId,
            @Valid @RequestBody OptionValueUpdateRequest request) {
        return ApiResponse.success(
                "Option value updated successfully",
                valueService.update(itemId, optionGroupId, optionValueId, request));
    }

    @PatchMapping("/{optionValueId}/available")
    public ApiResponse<OptionValueResponse> available(
            @PathVariable UUID itemId,
            @PathVariable UUID optionGroupId,
            @PathVariable UUID optionValueId) {
        return ApiResponse.success(
                "Option value marked available",
                valueService.markAvailable(itemId, optionGroupId, optionValueId));
    }

    @PatchMapping("/{optionValueId}/unavailable")
    public ApiResponse<OptionValueResponse> unavailable(
            @PathVariable UUID itemId,
            @PathVariable UUID optionGroupId,
            @PathVariable UUID optionValueId) {
        return ApiResponse.success(
                "Option value marked unavailable",
                valueService.markUnavailable(itemId, optionGroupId, optionValueId));
    }
}
