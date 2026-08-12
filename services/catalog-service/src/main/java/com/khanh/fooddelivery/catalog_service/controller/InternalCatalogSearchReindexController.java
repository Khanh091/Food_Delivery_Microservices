package com.khanh.fooddelivery.catalog_service.controller;

import com.khanh.fooddelivery.catalog_service.common.response.ApiResponse;
import com.khanh.fooddelivery.catalog_service.dto.response.CatalogSearchReindexResponse;
import com.khanh.fooddelivery.catalog_service.service.CatalogSearchReindexService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/catalog")
@RequiredArgsConstructor
public class InternalCatalogSearchReindexController {
    private final CatalogSearchReindexService reindexService;

    @PostMapping("/search-reindex")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CatalogSearchReindexResponse> reindex() {
        return ApiResponse.success("Catalog search snapshot queued", reindexService.enqueueCurrentCatalogSnapshot());
    }
}
