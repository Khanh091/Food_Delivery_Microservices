package com.khanh.fooddelivery.order_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "restaurant-service")
public interface RestaurantServiceClient {
    @GetMapping("/internal/v1/restaurants/{restaurantId}/branches/{branchId}/cart-availability")
    RemoteApiResponse<RestaurantBranchCartAvailabilityResponse> getCartAvailability(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable UUID restaurantId,
            @PathVariable UUID branchId);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RestaurantBranchCartAvailabilityResponse(
            UUID restaurantId, String restaurantName, boolean restaurantActive,
            UUID branchId, String branchName, boolean branchActive, boolean acceptingOrders) {}
}
