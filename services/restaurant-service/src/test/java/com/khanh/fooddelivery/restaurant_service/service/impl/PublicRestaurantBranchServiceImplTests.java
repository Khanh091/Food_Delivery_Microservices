package com.khanh.fooddelivery.restaurant_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.restaurant_service.dto.response.PublicRestaurantBranchResponse;
import com.khanh.fooddelivery.restaurant_service.entity.BranchBusinessHour;
import com.khanh.fooddelivery.restaurant_service.entity.Restaurant;
import com.khanh.fooddelivery.restaurant_service.entity.RestaurantBranch;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantBranchStatus;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantStatus;
import com.khanh.fooddelivery.restaurant_service.exception.AppException;
import com.khanh.fooddelivery.restaurant_service.exception.ErrorCode;
import com.khanh.fooddelivery.restaurant_service.repository.BranchBusinessHourRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantBranchRepository;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublicRestaurantBranchServiceImplTests {
    @Mock private RestaurantBranchRepository branches;
    @Mock private BranchBusinessHourRepository businessHours;

    @InjectMocks private PublicRestaurantBranchServiceImpl service;

    @Test
    void returnsOnlyCustomerFacingActiveBranchData() {
        UUID restaurantId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        RestaurantBranch branch = activeBranch(restaurantId, branchId);
        BranchBusinessHour hour = new BranchBusinessHour();
        hour.setDayOfWeek((short) 1);
        hour.setOpenTime(LocalTime.of(8, 0));
        hour.setCloseTime(LocalTime.of(22, 0));

        when(branches.findPublicByIdAndRestaurantId(restaurantId, branchId))
                .thenReturn(Optional.of(branch));
        when(businessHours.findAllByBranchIdOrderByDayOfWeek(branchId)).thenReturn(List.of(hour));

        PublicRestaurantBranchResponse response = service.get(restaurantId, branchId);

        assertThat(response.restaurantId()).isEqualTo(restaurantId);
        assertThat(response.branchId()).isEqualTo(branchId);
        assertThat(response.restaurantName()).isEqualTo("Public restaurant");
        assertThat(response.branchName()).isEqualTo("Public branch");
        assertThat(response.businessHours()).singleElement().satisfies(
                value -> {
                    assertThat(value.dayOfWeek()).isEqualTo((short) 1);
                    assertThat(value.openTime()).isEqualTo(LocalTime.of(8, 0));
                });
    }

    @Test
    void hidesInactiveBranchAsNotFound() {
        UUID restaurantId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        RestaurantBranch branch = activeBranch(restaurantId, branchId);
        branch.setStatus(RestaurantBranchStatus.INACTIVE);
        when(branches.findPublicByIdAndRestaurantId(restaurantId, branchId))
                .thenReturn(Optional.of(branch));

        assertThatThrownBy(() -> service.get(restaurantId, branchId))
                .isInstanceOf(AppException.class)
                .extracting(exception -> ((AppException) exception).getErrorCode())
                .isEqualTo(ErrorCode.BRANCH_NOT_FOUND);
    }

    @Test
    void hidesInactiveRestaurantAsNotFound() {
        UUID restaurantId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        RestaurantBranch branch = activeBranch(restaurantId, branchId);
        branch.getRestaurant().setStatus(RestaurantStatus.SUSPENDED);
        when(branches.findPublicByIdAndRestaurantId(restaurantId, branchId))
                .thenReturn(Optional.of(branch));

        assertThatThrownBy(() -> service.get(restaurantId, branchId))
                .isInstanceOf(AppException.class)
                .extracting(exception -> ((AppException) exception).getErrorCode())
                .isEqualTo(ErrorCode.BRANCH_NOT_FOUND);
    }

    @Test
    void returnsNotFoundWhenBranchDoesNotBelongToRestaurant() {
        UUID restaurantId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        when(branches.findPublicByIdAndRestaurantId(restaurantId, branchId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(restaurantId, branchId))
                .isInstanceOf(AppException.class)
                .extracting(exception -> ((AppException) exception).getErrorCode())
                .isEqualTo(ErrorCode.BRANCH_NOT_FOUND);
    }

    private RestaurantBranch activeBranch(UUID restaurantId, UUID branchId) {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(restaurantId);
        restaurant.setName("Public restaurant");
        restaurant.setStatus(RestaurantStatus.ACTIVE);

        RestaurantBranch branch = new RestaurantBranch();
        branch.setId(branchId);
        branch.setRestaurant(restaurant);
        branch.setName("Public branch");
        branch.setPhoneNumber("0900000000");
        branch.setAddressLine("1 Public street");
        branch.setStatus(RestaurantBranchStatus.ACTIVE);
        branch.setAcceptingOrders(true);
        return branch;
    }
}
