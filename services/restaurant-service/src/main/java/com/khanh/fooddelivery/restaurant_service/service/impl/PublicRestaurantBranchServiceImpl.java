package com.khanh.fooddelivery.restaurant_service.service.impl;

import com.khanh.fooddelivery.restaurant_service.dto.response.PublicBranchBusinessHourResponse;
import com.khanh.fooddelivery.restaurant_service.dto.response.PublicRestaurantBranchResponse;
import com.khanh.fooddelivery.restaurant_service.entity.RestaurantBranch;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantBranchStatus;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantStatus;
import com.khanh.fooddelivery.restaurant_service.exception.AppException;
import com.khanh.fooddelivery.restaurant_service.exception.ErrorCode;
import com.khanh.fooddelivery.restaurant_service.repository.BranchBusinessHourRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantBranchRepository;
import com.khanh.fooddelivery.restaurant_service.service.PublicRestaurantBranchService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicRestaurantBranchServiceImpl implements PublicRestaurantBranchService {
    private final RestaurantBranchRepository branches;
    private final BranchBusinessHourRepository businessHours;

    @Override
    public PublicRestaurantBranchResponse get(UUID restaurantId, UUID branchId) {
        RestaurantBranch branch =
                branches
                        .findPublicByIdAndRestaurantId(restaurantId, branchId)
                        .orElseThrow(() -> new AppException(ErrorCode.BRANCH_NOT_FOUND));
        if (branch.getRestaurant().getStatus() != RestaurantStatus.ACTIVE
                || branch.getStatus() != RestaurantBranchStatus.ACTIVE) {
            throw new AppException(ErrorCode.BRANCH_NOT_FOUND);
        }

        return new PublicRestaurantBranchResponse(
                restaurantId,
                branch.getRestaurant().getName(),
                branch.getRestaurant().getDescription(),
                branch.getRestaurant().getLogoUrl(),
                branch.getRestaurant().getCoverImageUrl(),
                branchId,
                branch.getName(),
                branch.getPhoneNumber(),
                branch.getAddressLine(),
                branch.getWard(),
                branch.getDistrict(),
                branch.getCity(),
                branch.isAcceptingOrders(),
                businessHours.findAllByBranchIdOrderByDayOfWeek(branchId).stream()
                        .map(
                                hour ->
                                        new PublicBranchBusinessHourResponse(
                                                hour.getDayOfWeek(),
                                                hour.getOpenTime(),
                                                hour.getCloseTime(),
                                                hour.isClosed()))
                        .toList());
    }
}
