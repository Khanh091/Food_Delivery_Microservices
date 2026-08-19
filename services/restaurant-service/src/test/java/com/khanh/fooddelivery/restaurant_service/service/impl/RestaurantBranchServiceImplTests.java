package com.khanh.fooddelivery.restaurant_service.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantBranchCreateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantBranchUpdateRequest;
import com.khanh.fooddelivery.restaurant_service.entity.Restaurant;
import com.khanh.fooddelivery.restaurant_service.entity.RestaurantBranch;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantBranchStatus;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantMemberRole;
import com.khanh.fooddelivery.restaurant_service.exception.AppException;
import com.khanh.fooddelivery.restaurant_service.exception.ErrorCode;
import com.khanh.fooddelivery.restaurant_service.mapper.BranchBusinessHourMapper;
import com.khanh.fooddelivery.restaurant_service.mapper.BranchSpecialHourMapper;
import com.khanh.fooddelivery.restaurant_service.mapper.RestaurantBranchMapper;
import com.khanh.fooddelivery.restaurant_service.outbox.OutboxEventService;
import com.khanh.fooddelivery.restaurant_service.repository.BranchBusinessHourRepository;
import com.khanh.fooddelivery.restaurant_service.repository.BranchSpecialHourRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantBranchRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantRepository;
import com.khanh.fooddelivery.restaurant_service.security.CurrentUserProvider;
import com.khanh.fooddelivery.restaurant_service.service.RestaurantAuthorizationService;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class RestaurantBranchServiceImplTests {
    private static final UUID RESTAURANT_ID = UUID.randomUUID();
    private static final UUID BRANCH_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock private RestaurantBranchRepository branches;
    @Mock private RestaurantRepository restaurants;
    @Mock private BranchBusinessHourRepository businessHours;
    @Mock private BranchSpecialHourRepository specialHours;
    @Mock private RestaurantBranchMapper branchMapper;
    @Mock private BranchBusinessHourMapper hourMapper;
    @Mock private BranchSpecialHourMapper specialMapper;
    @Mock private RestaurantAuthorizationService authorization;
    @Mock private CurrentUserProvider currentUser;
    @Mock private OutboxEventService outbox;
    @Mock private Jwt jwt;

    private RestaurantBranchServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RestaurantBranchServiceImpl(
                branches,
                restaurants,
                businessHours,
                specialHours,
                branchMapper,
                hourMapper,
                specialMapper,
                authorization,
                currentUser,
                outbox);
    }

    @Test
    void createRejectsUserWithoutRestaurantManagementRole() {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(RESTAURANT_ID);
        when(restaurants.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(currentUser.getCurrentUserId(jwt)).thenReturn(USER_ID);
        doThrow(new AppException(ErrorCode.RESTAURANT_ACCESS_DENIED))
                .when(authorization)
                .requireRestaurantAccess(
                        eq(RESTAURANT_ID),
                        eq(USER_ID),
                        any(RestaurantMemberRole[].class));

        RestaurantBranchCreateRequest request =
                new RestaurantBranchCreateRequest(
                        "B001",
                        "Branch A",
                        null,
                        null,
                        "Address",
                        null,
                        null,
                        "Hanoi",
                        new BigDecimal("21.0"),
                        new BigDecimal("105.8"),
                        BigDecimal.ZERO,
                        20);

        assertThatThrownBy(() -> service.create(jwt, RESTAURANT_ID, request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESTAURANT_ACCESS_DENIED);

        verify(branches, never()).save(any());
    }

    @Test
    void updateRejectsUserWithoutBranchAccess() {
        RestaurantBranch branch = new RestaurantBranch();
        branch.setId(BRANCH_ID);
        when(branches.findByIdForUpdate(BRANCH_ID)).thenReturn(Optional.of(branch));
        when(currentUser.getCurrentUserId(jwt)).thenReturn(USER_ID);
        doThrow(new AppException(ErrorCode.RESTAURANT_ACCESS_DENIED))
                .when(authorization)
                .requireBranchAccess(eq(BRANCH_ID), eq(USER_ID), any(RestaurantMemberRole[].class));

        RestaurantBranchUpdateRequest request =
                new RestaurantBranchUpdateRequest(
                        "Branch A",
                        null,
                        null,
                        "Address",
                        null,
                        null,
                        "Hanoi",
                        new BigDecimal("21.0"),
                        new BigDecimal("105.8"),
                        BigDecimal.ZERO,
                        20,
                        RestaurantBranchStatus.ACTIVE);

        assertThatThrownBy(() -> service.update(jwt, BRANCH_ID, request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESTAURANT_ACCESS_DENIED);

        verify(branchMapper, never()).update(any(), any());
    }
}