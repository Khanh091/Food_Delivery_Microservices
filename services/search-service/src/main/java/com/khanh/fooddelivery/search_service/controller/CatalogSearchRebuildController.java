package com.khanh.fooddelivery.search_service.controller;

import com.khanh.fooddelivery.search_service.client.CatalogSearchReindexClient.CatalogSnapshotResult;
import com.khanh.fooddelivery.search_service.service.CatalogSearchRebuildService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search/admin/catalog-items")
public class CatalogSearchRebuildController {
    private final CatalogSearchRebuildService rebuildService;

    public CatalogSearchRebuildController(CatalogSearchRebuildService rebuildService) {
        this.rebuildService = rebuildService;
    }

    @PostMapping("/rebuild")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CatalogSnapshotResult> rebuild() {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(rebuildService.rebuild());
    }
}
