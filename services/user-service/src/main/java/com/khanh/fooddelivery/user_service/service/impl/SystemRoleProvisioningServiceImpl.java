package com.khanh.fooddelivery.user_service.service.impl;

import com.khanh.fooddelivery.user_service.entity.User;
import com.khanh.fooddelivery.user_service.enums.SystemRole;
import com.khanh.fooddelivery.user_service.exception.AppException;
import com.khanh.fooddelivery.user_service.exception.ErrorCode;
import com.khanh.fooddelivery.user_service.identity.KeycloakAdminException;
import com.khanh.fooddelivery.user_service.identity.KeycloakRealmRoleClient;
import com.khanh.fooddelivery.user_service.repository.UserRepository;
import com.khanh.fooddelivery.user_service.service.SystemRoleProvisioningService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SystemRoleProvisioningServiceImpl implements SystemRoleProvisioningService {
    private final UserRepository users;
    private final KeycloakRealmRoleClient keycloakRoles;

    @Override
    @Transactional
    public boolean grantRole(UUID userId, SystemRole role) {
        requirePartnerGrantable(role);
        User user = requiredUser(userId);
        try {
            if (keycloakRoles.hasRealmRole(user.getKeycloakUserId(), role.name())) {
                return false;
            }
            keycloakRoles.grantRealmRole(user.getKeycloakUserId(), role.name());
            log.info("Granted system role: userId={}, role={}", userId, role);
            return true;
        } catch (KeycloakAdminException exception) {
            throw provisioningFailure(userId, role, exception);
        }
    }

    @Override
    @Transactional
    public boolean revokeRole(UUID userId, SystemRole role) {
        requirePartnerGrantable(role);
        User user = requiredUser(userId);
        try {
            if (!keycloakRoles.hasRealmRole(user.getKeycloakUserId(), role.name())) {
                return false;
            }
            keycloakRoles.revokeRealmRole(user.getKeycloakUserId(), role.name());
            log.info("Revoked system role: userId={}, role={}", userId, role);
            return true;
        } catch (KeycloakAdminException exception) {
            throw provisioningFailure(userId, role, exception);
        }
    }

    @Override
    public boolean hasRole(UUID userId, SystemRole role) {
        User user = requiredUser(userId);
        try {
            return keycloakRoles.hasRealmRole(user.getKeycloakUserId(), role.name());
        } catch (KeycloakAdminException exception) {
            throw provisioningFailure(userId, role, exception);
        }
    }

    private void requirePartnerGrantable(SystemRole role) {
        if (!role.isPartnerGrantable()) {
            throw new AppException(ErrorCode.SYSTEM_ROLE_NOT_GRANTABLE);
        }
    }

    private User requiredUser(UUID userId) {
        return users.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private AppException provisioningFailure(UUID userId, SystemRole role, Exception exception) {
        log.warn("System-role provisioning failed: userId={}, role={}", userId, role, exception);
        return new AppException(ErrorCode.SYSTEM_ROLE_PROVISIONING_FAILED);
    }
}
