package com.khanh.fooddelivery.order_service.client;

import com.khanh.fooddelivery.order_service.client.dto.response.OrderAuthorizationResponse;
import com.khanh.fooddelivery.order_service.client.dto.response.RestaurantBranchCartAvailabilityResponse;
import com.khanh.fooddelivery.order_service.common.response.ApiResponse;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "restaurant-service")
public interface RestaurantServiceClient {

    @GetMapping("/internal/v1/restaurants/{restaurantId}/branches/{branchId}/cart-availability")
    ApiResponse<RestaurantBranchCartAvailabilityResponse> getCartAvailability(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable UUID restaurantId,
            @PathVariable UUID branchId
    );

    @GetMapping("/internal/v1/restaurants/{restaurantId}/order-authorization")
    ApiResponse<OrderAuthorizationResponse> orderAuthorization(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable UUID restaurantId,
            @RequestParam(required = false) UUID branchId
    );

}
