package com.khanh.fooddelivery.catalog_service.controller;

import com.khanh.fooddelivery.catalog_service.common.response.ApiResponse;
import com.khanh.fooddelivery.catalog_service.dto.request.OptionGroupCreateRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.OptionGroupUpdateRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.OptionGroupResponse;
import com.khanh.fooddelivery.catalog_service.service.OptionGroupService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/catalog/items/{itemId}/option-groups")
@RequiredArgsConstructor
public class OptionGroupController {
    private final OptionGroupService groupService;

    @PostMapping
    public ResponseEntity<ApiResponse<OptionGroupResponse>> create(
            @PathVariable UUID itemId, @Valid @RequestBody OptionGroupCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Option group created successfully",
                                groupService.create(itemId, request)));
    }

    @GetMapping
    public ApiResponse<List<OptionGroupResponse>> list(@PathVariable UUID itemId) {
        return ApiResponse.success(
                "Option groups retrieved successfully", groupService.list(itemId));
    }

    @GetMapping("/{optionGroupId}")
    public ApiResponse<OptionGroupResponse> get(
            @PathVariable UUID itemId, @PathVariable UUID optionGroupId) {
        return ApiResponse.success(
                "Option group retrieved successfully", groupService.get(itemId, optionGroupId));
    }

    @PatchMapping("/{optionGroupId}")
    public ApiResponse<OptionGroupResponse> update(
            @PathVariable UUID itemId,
            @PathVariable UUID optionGroupId,
            @Valid @RequestBody OptionGroupUpdateRequest request) {
        return ApiResponse.success(
                "Option group updated successfully",
                groupService.update(itemId, optionGroupId, request));
    }

    @PatchMapping("/{optionGroupId}/activate")
    public ApiResponse<OptionGroupResponse> activate(
            @PathVariable UUID itemId, @PathVariable UUID optionGroupId) {
        return ApiResponse.success(
                "Option group activated", groupService.activate(itemId, optionGroupId));
    }

    @PatchMapping("/{optionGroupId}/deactivate")
    public ApiResponse<OptionGroupResponse> deactivate(
            @PathVariable UUID itemId, @PathVariable UUID optionGroupId) {
        return ApiResponse.success(
                "Option group deactivated", groupService.deactivate(itemId, optionGroupId));
    }
}
