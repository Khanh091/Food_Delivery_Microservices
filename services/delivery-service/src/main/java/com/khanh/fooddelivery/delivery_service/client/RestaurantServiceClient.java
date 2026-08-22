package com.khanh.fooddelivery.delivery_service.client;

import com.khanh.fooddelivery.delivery_service.client.dto.response.RestaurantBranchOrderingContextResponse;
import com.khanh.fooddelivery.delivery_service.common.response.ApiResponse;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "restaurant-service")
public interface RestaurantServiceClient {

    @GetMapping("/internal/v1/restaurants/branches/{branchId}/ordering-context")
    ApiResponse<RestaurantBranchOrderingContextResponse> getOrderingContext(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String internalApiKey,
            @PathVariable UUID branchId
    );
}
