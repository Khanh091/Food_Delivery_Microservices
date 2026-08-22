package com.khanh.fooddelivery.restaurant_service.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantBranchPublicAvailabilityResponse;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantBranchOrderingContextResponse;
import com.khanh.fooddelivery.restaurant_service.entity.Restaurant;
import com.khanh.fooddelivery.restaurant_service.entity.RestaurantBranch;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantBranchStatus;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantStatus;
import com.khanh.fooddelivery.restaurant_service.exception.AppException;
import com.khanh.fooddelivery.restaurant_service.exception.ErrorCode;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantBranchRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestaurantPublicAvailabilityServiceImplTests {
    private final UUID restaurantId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();

    @Mock private RestaurantRepository restaurantRepository;
    @Mock private RestaurantBranchRepository branchRepository;

    private RestaurantPublicAvailabilityServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RestaurantPublicAvailabilityServiceImpl(restaurantRepository, branchRepository);
    }

    @Test
    void activeRestaurantAndBranchAreVisibleWithoutMembershipChecks() {
        Restaurant restaurant = restaurant(RestaurantStatus.ACTIVE);
        RestaurantBranch branch = branch(RestaurantBranchStatus.ACTIVE);
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(branchRepository.findByIdAndRestaurantId(branchId, restaurantId))
                .thenReturn(Optional.of(branch));

        RestaurantBranchPublicAvailabilityResponse response =
                service.getBranchPublicAvailability(restaurantId, branchId);

        assertTrue(response.restaurantVisible());
        assertTrue(response.branchVisible());
    }

    @Test
    void pendingBranchIsNotCustomerVisible() {
        when(restaurantRepository.findById(restaurantId))
                .thenReturn(Optional.of(restaurant(RestaurantStatus.ACTIVE)));
        when(branchRepository.findByIdAndRestaurantId(branchId, restaurantId))
                .thenReturn(Optional.of(branch(RestaurantBranchStatus.PENDING)));

        RestaurantBranchPublicAvailabilityResponse response =
                service.getBranchPublicAvailability(restaurantId, branchId);

        assertEquals(false, response.branchVisible());
    }

    @Test
    void branchOutsideRestaurantScopeIsReportedAsNotFound() {
        when(restaurantRepository.findById(restaurantId))
                .thenReturn(Optional.of(restaurant(RestaurantStatus.ACTIVE)));
        when(branchRepository.findByIdAndRestaurantId(branchId, restaurantId))
                .thenReturn(Optional.empty());

        AppException error =
                assertThrows(
                        AppException.class,
                        () -> service.getBranchPublicAvailability(restaurantId, branchId));

        assertEquals(ErrorCode.BRANCH_NOT_FOUND, error.getErrorCode());
    }

    @Test
    void orderingContextResolvesRestaurantAndPickupCoordinatesFromBranchId() {
        Restaurant restaurant = restaurant(RestaurantStatus.ACTIVE);
        restaurant.setName("Restaurant");
        RestaurantBranch branch = branch(RestaurantBranchStatus.ACTIVE);
        branch.setRestaurant(restaurant);
        branch.setName("Branch");
        branch.setAcceptingOrders(true);
        branch.setAddressLine("120 Nguyen Trai");
        branch.setWard("Ward 1");
        branch.setDistrict("District 1");
        branch.setCity("Ho Chi Minh City");
        branch.setLatitude(java.math.BigDecimal.valueOf(10.75));
        branch.setLongitude(java.math.BigDecimal.valueOf(106.66));
        when(branchRepository.findByIdWithRestaurant(branchId)).thenReturn(Optional.of(branch));

        RestaurantBranchOrderingContextResponse response = service.getBranchOrderingContext(branchId);

        assertEquals(restaurantId, response.restaurantId());
        assertEquals(branchId, response.branchId());
        assertEquals(java.math.BigDecimal.valueOf(10.75), response.latitude());
        assertEquals("120 Nguyen Trai", response.addressLine());
        assertEquals("Ward 1", response.ward());
        assertEquals("District 1", response.district());
        assertEquals("Ho Chi Minh City", response.city());
        assertTrue(response.acceptingOrders());
    }

    private Restaurant restaurant(RestaurantStatus status) {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(restaurantId);
        restaurant.setStatus(status);
        return restaurant;
    }

    private RestaurantBranch branch(RestaurantBranchStatus status) {
        RestaurantBranch branch = new RestaurantBranch();
        branch.setId(branchId);
        branch.setStatus(status);
        return branch;
    }
}
