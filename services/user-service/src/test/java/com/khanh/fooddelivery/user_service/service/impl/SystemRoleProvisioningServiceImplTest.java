package com.khanh.fooddelivery.user_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.user_service.entity.User;
import com.khanh.fooddelivery.user_service.enums.SystemRole;
import com.khanh.fooddelivery.user_service.exception.AppException;
import com.khanh.fooddelivery.user_service.exception.ErrorCode;
import com.khanh.fooddelivery.user_service.identity.KeycloakRealmRoleClient;
import com.khanh.fooddelivery.user_service.repository.UserRepository;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SystemRoleProvisioningServiceImplTest {
    @Mock private UserRepository users;
    @Mock private KeycloakRealmRoleClient keycloakRoles;

    private SystemRoleProvisioningServiceImpl service;
    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        service = new SystemRoleProvisioningServiceImpl(users, keycloakRoles);
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setKeycloakUserId("keycloak-" + userId);
    }

    @Test
    void grantsRestaurantOwnerWhenNotAlreadyAssigned() {
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(keycloakRoles.hasRealmRole(user.getKeycloakUserId(), SystemRole.RESTAURANT_OWNER.name()))
                .thenReturn(false);

        assertThat(service.grantRole(userId, SystemRole.RESTAURANT_OWNER)).isTrue();
        verify(keycloakRoles).grantRealmRole(user.getKeycloakUserId(), SystemRole.RESTAURANT_OWNER.name());
    }

    @Test
    void repeatedRestaurantOwnerGrantIsIdempotentAndKeepsExistingCustomerRole() {
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(keycloakRoles.hasRealmRole(user.getKeycloakUserId(), SystemRole.RESTAURANT_OWNER.name()))
                .thenReturn(true);

        assertThat(service.grantRole(userId, SystemRole.RESTAURANT_OWNER)).isFalse();
        verify(keycloakRoles, never()).grantRealmRole(
                user.getKeycloakUserId(), SystemRole.RESTAURANT_OWNER.name());
        verify(keycloakRoles, never()).revokeRealmRole(
                user.getKeycloakUserId(), SystemRole.CUSTOMER.name());
    }

    @Test
    void customerRoleCannotBeChangedThroughPartnerRoleApi() {
        assertThatThrownBy(() -> service.grantRole(userId, SystemRole.CUSTOMER))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SYSTEM_ROLE_NOT_GRANTABLE);
    }

    @Test
    void partnerRoleContractDoesNotContainAdminOrSupport() {
        assertThat(Arrays.stream(SystemRole.values()).map(Enum::name))
                .doesNotContain("ADMIN", "SUPPORT");
    }
}