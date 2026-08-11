package com.khanh.fooddelivery.restaurant_service.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.restaurant_service.enums.RestaurantMemberRole;
import com.khanh.fooddelivery.restaurant_service.exception.AppException;
import com.khanh.fooddelivery.restaurant_service.exception.ErrorCode;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantBranchRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantRepository;
import com.khanh.fooddelivery.restaurant_service.security.CurrentUserProvider;
import com.khanh.fooddelivery.restaurant_service.service.RestaurantAuthorizationService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class RestaurantCatalogAuthorizationServiceImplTests {
    private static final UUID RESTAURANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock private RestaurantRepository restaurants;
    @Mock private RestaurantBranchRepository branches;
    @Mock private RestaurantAuthorizationService authorizationService;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private Jwt jwt;
    @Captor private ArgumentCaptor<RestaurantMemberRole[]> rolesCaptor;

    private RestaurantCatalogAuthorizationServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new RestaurantCatalogAuthorizationServiceImpl(
                        restaurants, branches, authorizationService, currentUserProvider);
    }

    @Test
    void delegatesTheExactCatalogManagementRolesToRestaurantAuthorization() {
        authorizedRestaurant();
        when(authorizationService.hasRestaurantRole(
                        eq(RESTAURANT_ID), eq(USER_ID), any(RestaurantMemberRole[].class)))
                .thenReturn(true);

        boolean authorized = service.authorizeCatalogAccess(jwt, RESTAURANT_ID, null).authorized();

        assertTrue(authorized);
        verify(authorizationService)
                .hasRestaurantRole(eq(RESTAURANT_ID), eq(USER_ID), rolesCaptor.capture());
        assertEquals(
                java.util.List.of(
                        RestaurantMemberRole.OWNER,
                        RestaurantMemberRole.MANAGER,
                        RestaurantMemberRole.CATALOG_MANAGER),
                java.util.List.of(rolesCaptor.getValue()));
    }

    @Test
    void nonMemberIsReturnedAsNotAuthorized() {
        authorizedRestaurant();
        when(authorizationService.hasRestaurantRole(
                        eq(RESTAURANT_ID), eq(USER_ID), any(RestaurantMemberRole[].class)))
                .thenReturn(false);

        boolean authorized = service.authorizeCatalogAccess(jwt, RESTAURANT_ID, null).authorized();

        assertFalse(authorized);
    }

    @Test
    void missingRestaurantIsReported() {
        UUID missingRestaurantId = UUID.randomUUID();
        when(restaurants.existsById(missingRestaurantId)).thenReturn(false);

        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> service.authorizeCatalogAccess(jwt, missingRestaurantId, null));

        assertEquals(ErrorCode.RESTAURANT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void branchThatDoesNotBelongToRestaurantIsReportedAsMissing() {
        UUID branchId = UUID.randomUUID();
        authorizedRestaurant();

        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> service.authorizeCatalogAccess(jwt, RESTAURANT_ID, branchId));

        assertEquals(ErrorCode.BRANCH_NOT_FOUND, exception.getErrorCode());
    }

    private void authorizedRestaurant() {
        when(restaurants.existsById(RESTAURANT_ID)).thenReturn(true);
        when(currentUserProvider.getCurrentUserId(jwt)).thenReturn(USER_ID);
    }
}
