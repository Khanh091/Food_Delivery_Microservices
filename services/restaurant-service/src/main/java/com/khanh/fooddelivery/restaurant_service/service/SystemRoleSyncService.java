package com.khanh.fooddelivery.restaurant_service.service;

import com.khanh.fooddelivery.restaurant_service.identity.SystemRole;
import java.util.UUID;

public interface SystemRoleSyncService {
    void enqueueGrant(UUID userId, SystemRole role);

    void enqueueRevoke(UUID userId, SystemRole role);

    void processDue();

    int reconcileRestaurantOwners();
}