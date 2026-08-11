package com.khanh.fooddelivery.catalog_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.khanh.fooddelivery.catalog_service.common.response.ApiResponse;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "restaurant-service")
public interface RestaurantServiceClient {
    @GetMapping("/internal/v1/restaurants/{restaurantId}/catalog-authorization")
    ApiResponse<CatalogAuthorizationResponse> getCatalogAuthorization(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable UUID restaurantId,
            @RequestParam(required = false) UUID branchId);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CatalogAuthorizationResponse(UUID restaurantId, UUID branchId, boolean authorized) {}
}
