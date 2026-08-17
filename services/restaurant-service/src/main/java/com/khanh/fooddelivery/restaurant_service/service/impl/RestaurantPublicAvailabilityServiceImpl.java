package com.khanh.fooddelivery.restaurant_service.service.impl;

import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantBranchPublicAvailabilityResponse;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantBranchCartAvailabilityResponse;
import com.khanh.fooddelivery.restaurant_service.entity.Restaurant;
import com.khanh.fooddelivery.restaurant_service.entity.RestaurantBranch;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantBranchStatus;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantStatus;
import com.khanh.fooddelivery.restaurant_service.exception.AppException;
import com.khanh.fooddelivery.restaurant_service.exception.ErrorCode;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantBranchRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantRepository;
import com.khanh.fooddelivery.restaurant_service.service.RestaurantPublicAvailabilityService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RestaurantPublicAvailabilityServiceImpl implements RestaurantPublicAvailabilityService {
    private final RestaurantRepository restaurants;
    private final RestaurantBranchRepository branches;

    @Override
    public RestaurantBranchPublicAvailabilityResponse getBranchPublicAvailability(
            UUID restaurantId, UUID branchId) {
        Restaurant restaurant =
                restaurants
                        .findById(restaurantId)
                        .orElseThrow(() -> new AppException(ErrorCode.RESTAURANT_NOT_FOUND));
        RestaurantBranch branch =
                branches
                        .findByIdAndRestaurantId(branchId, restaurantId)
                        .orElseThrow(() -> new AppException(ErrorCode.BRANCH_NOT_FOUND));

        return new RestaurantBranchPublicAvailabilityResponse(
                restaurant.getId(),
                branch.getId(),
                restaurant.getStatus() == RestaurantStatus.ACTIVE,
                branch.getStatus() == RestaurantBranchStatus.ACTIVE);
    }

    @Override
    public RestaurantBranchCartAvailabilityResponse getBranchCartAvailability(
            UUID restaurantId, UUID branchId) {
        Restaurant restaurant =
                restaurants
                        .findById(restaurantId)
                        .orElseThrow(() -> new AppException(ErrorCode.RESTAURANT_NOT_FOUND));
        RestaurantBranch branch =
                branches
                        .findByIdAndRestaurantId(branchId, restaurantId)
                        .orElseThrow(() -> new AppException(ErrorCode.BRANCH_NOT_FOUND));
        return new RestaurantBranchCartAvailabilityResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getStatus() == RestaurantStatus.ACTIVE,
                branch.getId(),
                branch.getName(),
                branch.getStatus() == RestaurantBranchStatus.ACTIVE,
                branch.isAcceptingOrders());
    }
}
