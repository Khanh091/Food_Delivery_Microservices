package com.khanh.fooddelivery.restaurant_service.service.impl;

import com.khanh.fooddelivery.restaurant_service.dto.response.CatalogAuthorizationResponse;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantMemberRole;
import com.khanh.fooddelivery.restaurant_service.exception.AppException;
import com.khanh.fooddelivery.restaurant_service.exception.ErrorCode;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantBranchRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantRepository;
import com.khanh.fooddelivery.restaurant_service.security.CurrentUserProvider;
import com.khanh.fooddelivery.restaurant_service.service.RestaurantAuthorizationService;
import com.khanh.fooddelivery.restaurant_service.service.RestaurantCatalogAuthorizationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RestaurantCatalogAuthorizationServiceImpl
        implements RestaurantCatalogAuthorizationService {
    private static final RestaurantMemberRole[] CATALOG_MANAGEMENT_ROLES = {
        RestaurantMemberRole.OWNER,
        RestaurantMemberRole.MANAGER,
        RestaurantMemberRole.CATALOG_MANAGER
    };

    private final RestaurantRepository restaurants;
    private final RestaurantBranchRepository branches;
    private final RestaurantAuthorizationService authorizationService;
    private final CurrentUserProvider currentUserProvider;

    @Override
    public CatalogAuthorizationResponse authorizeCatalogAccess(
            Jwt jwt, UUID restaurantId, UUID branchId) {
        requireRestaurant(restaurantId);
        UUID userId = currentUserProvider.getCurrentUserId(jwt);
        boolean authorized =
                branchId == null
                        ? authorizationService.hasRestaurantRole(
                                restaurantId, userId, CATALOG_MANAGEMENT_ROLES)
                        : authorizationService.hasBranchAccess(
                                requireBranchForRestaurant(restaurantId, branchId),
                                userId,
                                CATALOG_MANAGEMENT_ROLES);
        return new CatalogAuthorizationResponse(restaurantId, branchId, authorized);
    }

    private void requireRestaurant(UUID restaurantId) {
        if (!restaurants.existsById(restaurantId)) {
            throw new AppException(ErrorCode.RESTAURANT_NOT_FOUND);
        }
    }

    private UUID requireBranchForRestaurant(UUID restaurantId, UUID branchId) {
        return branches.findByIdAndRestaurantId(branchId, restaurantId)
                .map(restaurantBranch -> restaurantBranch.getId())
                .orElseThrow(() -> new AppException(ErrorCode.BRANCH_NOT_FOUND));
    }
}
