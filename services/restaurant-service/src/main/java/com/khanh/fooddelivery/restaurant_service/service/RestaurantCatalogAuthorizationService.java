package com.khanh.fooddelivery.restaurant_service.service;

import com.khanh.fooddelivery.restaurant_service.dto.response.CatalogAuthorizationResponse;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

public interface RestaurantCatalogAuthorizationService {
    CatalogAuthorizationResponse authorizeCatalogAccess(Jwt jwt, UUID restaurantId, UUID branchId);
}
