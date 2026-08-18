package com.khanh.fooddelivery.user_service.service;

import com.khanh.fooddelivery.user_service.enums.SystemRole;
import java.util.UUID;

public interface SystemRoleProvisioningService {
    boolean grantRole(UUID userId, SystemRole role);

    boolean revokeRole(UUID userId, SystemRole role);

    boolean hasRole(UUID userId, SystemRole role);
}
