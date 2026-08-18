package com.khanh.fooddelivery.user_service.identity;

public interface KeycloakRealmRoleClient {
    boolean hasRealmRole(String keycloakUserId, String roleName);

    void grantRealmRole(String keycloakUserId, String roleName);

    void revokeRealmRole(String keycloakUserId, String roleName);
}
