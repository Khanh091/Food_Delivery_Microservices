package com.khanh.fooddelivery.catalog_service.service.impl;

import com.khanh.fooddelivery.catalog_service.client.RestaurantServiceClient;
import com.khanh.fooddelivery.catalog_service.common.response.ApiResponse;
import com.khanh.fooddelivery.catalog_service.exception.AppException;
import com.khanh.fooddelivery.catalog_service.exception.ErrorCode;
import com.khanh.fooddelivery.catalog_service.security.CurrentBearerTokenProvider;
import com.khanh.fooddelivery.catalog_service.service.CatalogAuthorizationService;
import feign.FeignException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CatalogAuthorizationServiceImpl implements CatalogAuthorizationService {
    private final RestaurantServiceClient restaurantServiceClient;
    private final CurrentBearerTokenProvider currentBearerTokenProvider;

    @Override
    public void requireRestaurantCatalogAccess(UUID restaurantId) {
        requireCatalogAccess(restaurantId, null);
    }

    @Override
    public void requireBranchCatalogAccess(UUID restaurantId, UUID branchId) {
        requireCatalogAccess(restaurantId, branchId);
    }

    private void requireCatalogAccess(UUID restaurantId, UUID branchId) {
        try {
            ApiResponse<RestaurantServiceClient.CatalogAuthorizationResponse> response =
                    restaurantServiceClient.getCatalogAuthorization(
                            currentBearerTokenProvider.getBearerToken(), restaurantId, branchId);
            if (response == null || !response.success() || response.data() == null) {
                throw new AppException(ErrorCode.RESTAURANT_SERVICE_UNAVAILABLE);
            }
            if (!response.data().authorized()) {
                throw new AppException(ErrorCode.ACCESS_DENIED);
            }
        } catch (FeignException exception) {
            throw mapFeignException(exception, branchId);
        }
    }

    private AppException mapFeignException(FeignException exception, UUID branchId) {
        return switch (exception.status()) {
            case 401 -> new AppException(ErrorCode.UNAUTHENTICATED);
            case 403 -> new AppException(ErrorCode.ACCESS_DENIED);
            case 404 ->
                    new AppException(
                            branchId == null
                                    ? ErrorCode.RESTAURANT_NOT_FOUND
                                    : ErrorCode.BRANCH_NOT_FOUND);
            default -> new AppException(ErrorCode.RESTAURANT_SERVICE_UNAVAILABLE);
        };
    }
}
