package com.khanh.fooddelivery.order_service.service;

import java.util.UUID;

public interface RestaurantAuthorizationService {
    void requireAccess(UUID restaurantId, UUID branchId);
}
