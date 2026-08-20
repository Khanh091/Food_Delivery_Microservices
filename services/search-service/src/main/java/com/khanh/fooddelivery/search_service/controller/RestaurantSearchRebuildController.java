package com.khanh.fooddelivery.search_service.controller;

import com.khanh.fooddelivery.search_service.client.RestaurantSearchReindexClient.RestaurantSnapshotResult;
import com.khanh.fooddelivery.search_service.service.RestaurantSearchRebuildService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search/admin/restaurants")
@RequiredArgsConstructor
public class RestaurantSearchRebuildController {

    private final RestaurantSearchRebuildService service;

    @PostMapping("/rebuild")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestaurantSnapshotResult> rebuild() {
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(service.rebuild());
    }
}