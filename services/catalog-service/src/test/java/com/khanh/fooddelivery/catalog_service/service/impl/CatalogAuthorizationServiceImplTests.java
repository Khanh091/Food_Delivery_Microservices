package com.khanh.fooddelivery.catalog_service.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.catalog_service.client.RestaurantServiceClient;
import com.khanh.fooddelivery.catalog_service.common.response.ApiResponse;
import com.khanh.fooddelivery.catalog_service.exception.AppException;
import com.khanh.fooddelivery.catalog_service.exception.ErrorCode;
import com.khanh.fooddelivery.catalog_service.security.CurrentBearerTokenProvider;
import feign.FeignException;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CatalogAuthorizationServiceImplTests {
    private static final UUID RESTAURANT_ID = UUID.randomUUID();
    private static final UUID BRANCH_ID = UUID.randomUUID();

    @Mock private RestaurantServiceClient restaurantServiceClient;
    @Mock private CurrentBearerTokenProvider currentBearerTokenProvider;

    private CatalogAuthorizationServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new CatalogAuthorizationServiceImpl(
                        restaurantServiceClient, currentBearerTokenProvider);
        when(currentBearerTokenProvider.getBearerToken())
                .thenReturn("Bearer original-access-token");
    }

    @ParameterizedTest(name = "{0} is allowed when restaurant-service authorizes catalog access")
    @MethodSource("catalogManagers")
    void catalogManagersAreAllowed(String role) {
        when(restaurantServiceClient.getCatalogAuthorization(
                        "Bearer original-access-token", RESTAURANT_ID, null))
                .thenReturn(authorized(RESTAURANT_ID, null));

        assertDoesNotThrow(() -> service.requireRestaurantCatalogAccess(RESTAURANT_ID));

        verify(restaurantServiceClient)
                .getCatalogAuthorization("Bearer original-access-token", RESTAURANT_ID, null);
    }

    @ParameterizedTest(name = "{0} is denied when restaurant-service denies catalog access")
    @MethodSource("catalogNonManagers")
    void catalogNonManagersAreDenied(String role) {
        when(restaurantServiceClient.getCatalogAuthorization(
                        "Bearer original-access-token", RESTAURANT_ID, null))
                .thenReturn(denied(RESTAURANT_ID, null));

        AppException exception =
                assertThrows(
                        AppException.class,
                        () -> service.requireRestaurantCatalogAccess(RESTAURANT_ID));

        assertEquals(ErrorCode.ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    void branchAccessForwardsOriginalBearerToken() {
        when(restaurantServiceClient.getCatalogAuthorization(
                        "Bearer original-access-token", RESTAURANT_ID, BRANCH_ID))
                .thenReturn(authorized(RESTAURANT_ID, BRANCH_ID));

        assertDoesNotThrow(() -> service.requireBranchCatalogAccess(RESTAURANT_ID, BRANCH_ID));

        verify(restaurantServiceClient)
                .getCatalogAuthorization("Bearer original-access-token", RESTAURANT_ID, BRANCH_ID);
    }

    @Test
    void missingRestaurantIsMapped() {
        FeignException exception = mock(FeignException.class);
        when(exception.status()).thenReturn(404);
        when(restaurantServiceClient.getCatalogAuthorization(any(), eq(RESTAURANT_ID), any()))
                .thenThrow(exception);

        AppException mapped =
                assertThrows(
                        AppException.class,
                        () -> service.requireRestaurantCatalogAccess(RESTAURANT_ID));

        assertEquals(ErrorCode.RESTAURANT_NOT_FOUND, mapped.getErrorCode());
    }

    @Test
    void missingOrMismatchedBranchIsMapped() {
        FeignException exception = mock(FeignException.class);
        when(exception.status()).thenReturn(404);
        when(restaurantServiceClient.getCatalogAuthorization(
                        any(), eq(RESTAURANT_ID), eq(BRANCH_ID)))
                .thenThrow(exception);

        AppException mapped =
                assertThrows(
                        AppException.class,
                        () -> service.requireBranchCatalogAccess(RESTAURANT_ID, BRANCH_ID));

        assertEquals(ErrorCode.BRANCH_NOT_FOUND, mapped.getErrorCode());
    }

    @Test
    void unavailableRestaurantServiceIsMapped() {
        FeignException exception = mock(FeignException.class);
        when(exception.status()).thenReturn(503);
        when(restaurantServiceClient.getCatalogAuthorization(any(), eq(RESTAURANT_ID), any()))
                .thenThrow(exception);

        AppException mapped =
                assertThrows(
                        AppException.class,
                        () -> service.requireRestaurantCatalogAccess(RESTAURANT_ID));

        assertEquals(ErrorCode.RESTAURANT_SERVICE_UNAVAILABLE, mapped.getErrorCode());
    }

    private static Stream<Arguments> catalogManagers() {
        return Stream.of(
                Arguments.of("OWNER"), Arguments.of("MANAGER"), Arguments.of("CATALOG_MANAGER"));
    }

    private static Stream<Arguments> catalogNonManagers() {
        return Stream.of(
                Arguments.of("ORDER_OPERATOR"),
                Arguments.of("ACCOUNTANT"),
                Arguments.of("STAFF"),
                Arguments.of("CUSTOMER"));
    }

    private static ApiResponse<RestaurantServiceClient.CatalogAuthorizationResponse> authorized(
            UUID restaurantId, UUID branchId) {
        return new ApiResponse<>(
                true,
                "SUCCESS",
                "Catalog authorization resolved",
                new RestaurantServiceClient.CatalogAuthorizationResponse(
                        restaurantId, branchId, true),
                null);
    }

    private static ApiResponse<RestaurantServiceClient.CatalogAuthorizationResponse> denied(
            UUID restaurantId, UUID branchId) {
        return new ApiResponse<>(
                true,
                "SUCCESS",
                "Catalog authorization resolved",
                new RestaurantServiceClient.CatalogAuthorizationResponse(
                        restaurantId, branchId, false),
                null);
    }
}
