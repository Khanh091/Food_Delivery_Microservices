package com.khanh.fooddelivery.restaurant_service.controller;

import com.khanh.fooddelivery.restaurant_service.common.response.ApiResponse;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantBranchPublicAvailabilityResponse;
import com.khanh.fooddelivery.restaurant_service.service.RestaurantPublicAvailabilityService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/restaurants")
@RequiredArgsConstructor
public class InternalRestaurantPublicAvailabilityController {
    private final RestaurantPublicAvailabilityService publicAvailabilityService;

    @GetMapping("/{restaurantId}/branches/{branchId}/public-availability")
    public ApiResponse<RestaurantBranchPublicAvailabilityResponse> getPublicAvailability(
            @PathVariable UUID restaurantId, @PathVariable UUID branchId) {
        return ApiResponse.success(
                "Restaurant branch public availability resolved",
                publicAvailabilityService.getBranchPublicAvailability(restaurantId, branchId));
    }
}
