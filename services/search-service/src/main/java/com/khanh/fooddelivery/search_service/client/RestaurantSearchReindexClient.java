package com.khanh.fooddelivery.search_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "restaurant-service")
public interface RestaurantSearchReindexClient {
    @PostMapping("/internal/v1/restaurants/search-reindex")
    RestaurantSnapshotResult triggerSearchReindex(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization);
    record RestaurantSnapshotResult(long restaurantsQueued, long branchesQueued) {}
}
