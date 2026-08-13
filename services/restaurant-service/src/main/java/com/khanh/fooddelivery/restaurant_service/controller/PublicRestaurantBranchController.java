package com.khanh.fooddelivery.restaurant_service.controller;

import com.khanh.fooddelivery.restaurant_service.common.response.ApiResponse;
import com.khanh.fooddelivery.restaurant_service.dto.response.PublicRestaurantBranchResponse;
import com.khanh.fooddelivery.restaurant_service.service.PublicRestaurantBranchService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/restaurants/{restaurantId}/branches/{branchId}")
@RequiredArgsConstructor
public class PublicRestaurantBranchController {
    private final PublicRestaurantBranchService service;

    @GetMapping
    public ApiResponse<PublicRestaurantBranchResponse> get(
            @PathVariable UUID restaurantId, @PathVariable UUID branchId) {
        return ApiResponse.success(
                "Public restaurant branch retrieved", service.get(restaurantId, branchId));
    }
}
