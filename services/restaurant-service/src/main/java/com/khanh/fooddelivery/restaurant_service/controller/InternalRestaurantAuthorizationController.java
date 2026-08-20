package com.khanh.fooddelivery.restaurant_service.controller;

import com.khanh.fooddelivery.restaurant_service.common.response.ApiResponse;
import com.khanh.fooddelivery.restaurant_service.dto.response.CatalogAuthorizationResponse;
import com.khanh.fooddelivery.restaurant_service.service.RestaurantCatalogAuthorizationService;
import java.util.UUID;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantMemberRole;
import com.khanh.fooddelivery.restaurant_service.service.RestaurantAuthorizationService;
import com.khanh.fooddelivery.restaurant_service.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/restaurants")
@RequiredArgsConstructor
public class InternalRestaurantAuthorizationController {
    private final RestaurantCatalogAuthorizationService catalogAuthorizationService;
    private final RestaurantAuthorizationService restaurantAuthorizationService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/{restaurantId}/catalog-authorization")
    public ApiResponse<CatalogAuthorizationResponse> authorizeCatalogAccess(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID restaurantId,
            @RequestParam(required = false) UUID branchId) {
        return ApiResponse.success(
                "Catalog authorization resolved",
                catalogAuthorizationService.authorizeCatalogAccess(jwt, restaurantId, branchId));
    }

    @GetMapping("/{restaurantId}/order-authorization")
    public ApiResponse<CatalogAuthorizationResponse> authorizeOrderAccess(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID restaurantId,
            @RequestParam(required = false) UUID branchId) {
        UUID userId = currentUserProvider.getCurrentUserId(jwt);
        boolean authorized = branchId == null
                ? restaurantAuthorizationService.hasRestaurantRole(restaurantId, userId,
                        RestaurantMemberRole.OWNER, RestaurantMemberRole.MANAGER, RestaurantMemberRole.ORDER_OPERATOR)
                : restaurantAuthorizationService.hasBranchAccess(branchId, userId,
                        RestaurantMemberRole.OWNER, RestaurantMemberRole.MANAGER, RestaurantMemberRole.ORDER_OPERATOR);
        return ApiResponse.success("Order authorization resolved", new CatalogAuthorizationResponse(restaurantId, branchId, authorized));
    }
}
