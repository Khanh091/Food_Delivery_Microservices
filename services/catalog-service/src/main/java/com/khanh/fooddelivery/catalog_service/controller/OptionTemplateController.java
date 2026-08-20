package com.khanh.fooddelivery.catalog_service.controller;

import com.khanh.fooddelivery.catalog_service.common.response.ApiResponse;
import com.khanh.fooddelivery.catalog_service.dto.request.OptionTemplateUpsertRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.OptionTemplateBatchCopyRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.OptionGroupResponse;
import com.khanh.fooddelivery.catalog_service.dto.response.OptionTemplatePageResponse;
import com.khanh.fooddelivery.catalog_service.dto.response.OptionTemplateResponse;
import com.khanh.fooddelivery.catalog_service.service.OptionTemplateService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/catalog/restaurants/{restaurantId}/option-templates")
public class OptionTemplateController {
    private final OptionTemplateService templateService;

    @GetMapping
    public ApiResponse<OptionTemplatePageResponse> list(
            @PathVariable UUID restaurantId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return ApiResponse.success("Option templates retrieved successfully", templateService.list(restaurantId, q, page, size));
    }

    @GetMapping("/{templateId}")
    public ApiResponse<OptionTemplateResponse> get(@PathVariable UUID restaurantId, @PathVariable UUID templateId) {
        return ApiResponse.success("Option template retrieved successfully", templateService.get(restaurantId, templateId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OptionTemplateResponse>> create(
            @PathVariable UUID restaurantId, @Valid @RequestBody OptionTemplateUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Option template created successfully", templateService.create(restaurantId, request)));
    }

    @PatchMapping("/{templateId}")
    public ApiResponse<OptionTemplateResponse> update(
            @PathVariable UUID restaurantId, @PathVariable UUID templateId,
            @Valid @RequestBody OptionTemplateUpsertRequest request) {
        return ApiResponse.success("Option template updated successfully", templateService.update(restaurantId, templateId, request));
    }

    @PatchMapping("/{templateId}/activate")
    public ApiResponse<OptionTemplateResponse> activate(@PathVariable UUID restaurantId, @PathVariable UUID templateId) {
        return ApiResponse.success("Option template activated", templateService.activate(restaurantId, templateId));
    }

    @PatchMapping("/{templateId}/deactivate")
    public ApiResponse<OptionTemplateResponse> deactivate(@PathVariable UUID restaurantId, @PathVariable UUID templateId) {
        return ApiResponse.success("Option template deactivated", templateService.deactivate(restaurantId, templateId));
    }

    @PostMapping("/{templateId}/copy-to-items/{itemId}")
    public ResponseEntity<ApiResponse<OptionGroupResponse>> copyToItem(
            @PathVariable UUID restaurantId, @PathVariable UUID templateId, @PathVariable UUID itemId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Option template copied to catalog item", templateService.copyToItem(restaurantId, templateId, itemId)));
    }

    @PostMapping("/copy-to-items/{itemId}/batch")
    public ResponseEntity<ApiResponse<List<OptionGroupResponse>>> copyToItemBatch(
            @PathVariable UUID restaurantId,
            @PathVariable UUID itemId,
            @Valid @RequestBody OptionTemplateBatchCopyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Option templates copied to catalog item",
                templateService.copyToItemBatch(restaurantId, itemId, request)));
    }
}
