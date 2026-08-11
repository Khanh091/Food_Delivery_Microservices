package com.khanh.fooddelivery.catalog_service.controller;

import com.khanh.fooddelivery.catalog_service.common.response.ApiResponse;
import com.khanh.fooddelivery.catalog_service.dto.request.ItemImageSortOrderUpdateRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.ItemImageResponse;
import com.khanh.fooddelivery.catalog_service.service.ItemImageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/catalog/items/{itemId}/images")
@RequiredArgsConstructor
@Validated
public class ItemImageController {
    private final ItemImageService imageService;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<ItemImageResponse>> upload(
            @PathVariable UUID itemId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) @Min(0) Integer sortOrder,
            @RequestParam(required = false) Boolean isPrimary) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Item image uploaded successfully",
                                imageService.upload(itemId, file, sortOrder, isPrimary)));
    }

    @GetMapping
    public ApiResponse<List<ItemImageResponse>> list(@PathVariable UUID itemId) {
        return ApiResponse.success("Item images retrieved successfully", imageService.list(itemId));
    }

    @PatchMapping("/{imageId}/primary")
    public ApiResponse<ItemImageResponse> setPrimary(
            @PathVariable UUID itemId, @PathVariable UUID imageId) {
        return ApiResponse.success(
                "Item image marked primary", imageService.setPrimary(itemId, imageId));
    }

    @PatchMapping("/{imageId}/sort-order")
    public ApiResponse<ItemImageResponse> updateSortOrder(
            @PathVariable UUID itemId,
            @PathVariable UUID imageId,
            @Valid @RequestBody ItemImageSortOrderUpdateRequest request) {
        return ApiResponse.success(
                "Item image sort order updated",
                imageService.updateSortOrder(itemId, imageId, request.sortOrder()));
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> delete(@PathVariable UUID itemId, @PathVariable UUID imageId) {
        imageService.delete(itemId, imageId);
        return ResponseEntity.noContent().build();
    }
}
