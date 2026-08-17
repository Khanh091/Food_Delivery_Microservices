package com.khanh.fooddelivery.delivery_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "restaurant-service")
public interface RestaurantServiceClient {
    @GetMapping("/internal/v1/restaurants/branches/{branchId}/ordering-context")
    RemoteApiResponse<RestaurantBranchOrderingContextResponse> getOrderingContext(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization, @PathVariable UUID branchId);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RestaurantBranchOrderingContextResponse(
            UUID restaurantId, String restaurantName, boolean restaurantActive, UUID branchId, String branchName,
            boolean branchActive, boolean acceptingOrders, BigDecimal latitude, BigDecimal longitude) {}
}
