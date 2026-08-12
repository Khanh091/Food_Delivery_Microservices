package com.khanh.fooddelivery.search_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "catalog-service")
public interface CatalogSearchReindexClient {
    @PostMapping("/internal/v1/catalog/search-reindex")
    CatalogReindexResponse triggerSearchReindex(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CatalogReindexResponse(
            boolean success,
            String code,
            String message,
            CatalogSnapshotResult data,
            Instant timestamp) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CatalogSnapshotResult(long catalogItemsQueued, long branchItemsQueued) {}
}
