package com.khanh.fooddelivery.restaurant_service.controller;

import com.khanh.fooddelivery.restaurant_service.common.response.ApiResponse;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantBranchPublicAvailabilityResponse;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantBranchCartAvailabilityResponse;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantBranchOrderingContextResponse;
import com.khanh.fooddelivery.restaurant_service.service.RestaurantPublicAvailabilityService;
import com.khanh.fooddelivery.restaurant_service.security.InternalRequestAuthenticator;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/internal/v1/restaurants")
@RequiredArgsConstructor
public class InternalRestaurantPublicAvailabilityController {
    private final RestaurantPublicAvailabilityService publicAvailabilityService;
    private final InternalRequestAuthenticator internalRequests;

    @GetMapping("/{restaurantId}/branches/{branchId}/public-availability")
    public ApiResponse<RestaurantBranchPublicAvailabilityResponse> getPublicAvailability(
            @PathVariable UUID restaurantId, @PathVariable UUID branchId) {
        return ApiResponse.success(
                "Restaurant branch public availability resolved",
                publicAvailabilityService.getBranchPublicAvailability(restaurantId, branchId));
    }

    @GetMapping("/{restaurantId}/branches/{branchId}/cart-availability")
    public ApiResponse<RestaurantBranchCartAvailabilityResponse> getCartAvailability(
            @PathVariable UUID restaurantId, @PathVariable UUID branchId) {
        return ApiResponse.success(
                "Restaurant branch cart availability resolved",
                publicAvailabilityService.getBranchCartAvailability(restaurantId, branchId));
    }

    @GetMapping("/branches/{branchId}/ordering-context")
    public ApiResponse<RestaurantBranchOrderingContextResponse> getOrderingContext(
            @PathVariable UUID branchId,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String internalApiKey,
            @AuthenticationPrincipal Jwt jwt) {
        internalRequests.authenticate(internalApiKey, jwt);
        return ApiResponse.success(
                "Restaurant branch ordering context resolved",
                publicAvailabilityService.getBranchOrderingContext(branchId));
    }
}
