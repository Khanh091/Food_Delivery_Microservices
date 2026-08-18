package com.khanh.fooddelivery.restaurant_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.restaurant_service.entity.Restaurant;
import com.khanh.fooddelivery.restaurant_service.entity.RestaurantBranch;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantMemberRole;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantBranchRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantMemberRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestaurantAuthorizationServiceImplTests {
    @Mock private RestaurantRepository restaurants;
    @Mock private RestaurantMemberRepository members;
    @Mock private RestaurantBranchRepository branches;

    private RestaurantAuthorizationServiceImpl service;
    private UUID ownerId;
    private UUID restaurantA;
    private UUID restaurantB;

    @BeforeEach
    void setUp() {
        service = new RestaurantAuthorizationServiceImpl(restaurants, members, branches);
        ownerId = UUID.randomUUID();
        restaurantA = UUID.randomUUID();
        restaurantB = UUID.randomUUID();
    }

    @Test
    void restaurantOwnerRoleDoesNotAuthorizeAnotherRestaurant() {
        Restaurant owned = new Restaurant();
        owned.setOwnerUserId(ownerId);
        owned.setId(restaurantA);
        when(restaurants.findById(restaurantA)).thenReturn(Optional.of(owned));
        when(restaurants.findById(restaurantB)).thenReturn(Optional.empty());

        assertThat(service.hasRestaurantRole(
                        restaurantA, ownerId, RestaurantMemberRole.OWNER))
                .isTrue();
        assertThat(service.hasRestaurantRole(
                        restaurantB, ownerId, RestaurantMemberRole.OWNER))
                .isFalse();
    }

    @Test
    void branchMembershipIsOnlyEvaluatedAgainstTheSpecificBranchRestaurant() {
        Restaurant restaurant = new Restaurant();
        restaurant.setOwnerUserId(UUID.randomUUID());
        restaurant.setId(restaurantA);
        RestaurantBranch branch = new RestaurantBranch();
        branch.setRestaurant(restaurant);
        UUID branchId = UUID.randomUUID();
        when(branches.findById(branchId)).thenReturn(Optional.of(branch));
        when(members.existsByBranchIdAndUserIdAndStatusAndRoleIn(
                        branchId,
                        ownerId,
                        com.khanh.fooddelivery.restaurant_service.enums.RestaurantMemberStatus.ACTIVE,
                        List.of(RestaurantMemberRole.OWNER)))
                .thenReturn(false);

        assertThat(service.hasBranchAccess(branchId, ownerId, RestaurantMemberRole.OWNER)).isFalse();
    }
}