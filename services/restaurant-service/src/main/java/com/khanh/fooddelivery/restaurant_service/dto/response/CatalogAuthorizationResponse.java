package com.khanh.fooddelivery.restaurant_service.dto.response;

import java.util.UUID;

public record CatalogAuthorizationResponse(UUID restaurantId, UUID branchId, boolean authorized) {}
