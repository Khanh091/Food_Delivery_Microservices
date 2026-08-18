package com.khanh.fooddelivery.user_service.identity;

import com.khanh.fooddelivery.user_service.config.KeycloakAdminProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakAdminRealmRoleClient implements KeycloakRealmRoleClient {
    private final KeycloakAdminProperties properties;
    private final Clock clock;

    private volatile AccessToken accessToken;

    @Override
    public boolean hasRealmRole(String keycloakUserId, String roleName) {
        try {
            List<RoleRepresentation> roles =
                    adminClient()
                            .get()
                            .uri(
                                    "/admin/realms/{realm}/users/{userId}/role-mappings/realm",
                                    properties.getRealm(),
                                    keycloakUserId)
                            .retrieve()
                            .body(new org.springframework.core.ParameterizedTypeReference<>() {});
            return roles != null && roles.stream().anyMatch(role -> roleName.equalsIgnoreCase(role.name()));
        } catch (RestClientException exception) {
            throw translate("read role mappings", keycloakUserId, roleName, exception);
        }
    }

    @Override
    public void grantRealmRole(String keycloakUserId, String roleName) {
        updateRealmRole(keycloakUserId, roleName, true);
    }

    @Override
    public void revokeRealmRole(String keycloakUserId, String roleName) {
        updateRealmRole(keycloakUserId, roleName, false);
    }

    private void updateRealmRole(String keycloakUserId, String roleName, boolean grant) {
        RoleRepresentation role = realmRole(roleName);
        try {
            RestClient client = adminClient();
            if (grant) {
                client.post()
                        .uri(
                                "/admin/realms/{realm}/users/{userId}/role-mappings/realm",
                                properties.getRealm(),
                                keycloakUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(List.of(role))
                        .retrieve()
                        .toBodilessEntity();
            } else {
                client.method(HttpMethod.DELETE)
                        .uri(
                                "/admin/realms/{realm}/users/{userId}/role-mappings/realm",
                                properties.getRealm(),
                                keycloakUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(List.of(role))
                        .retrieve()
                        .toBodilessEntity();
            }
        } catch (RestClientException exception) {
            throw translate(grant ? "grant role" : "revoke role", keycloakUserId, roleName, exception);
        }
    }

    private RoleRepresentation realmRole(String roleName) {
        try {
            RoleRepresentation role =
                    adminClient()
                            .get()
                            .uri("/admin/realms/{realm}/roles/{role}", properties.getRealm(), roleName)
                            .retrieve()
                            .body(RoleRepresentation.class);
            if (role == null || role.id() == null || role.name() == null) {
                throw new KeycloakAdminException("Keycloak realm role is not configured: " + roleName);
            }
            return role;
        } catch (RestClientException exception) {
            throw translate("read realm role", null, roleName, exception);
        }
    }

    private RestClient adminClient() {
        return RestClient.builder()
                .baseUrl(trimTrailingSlash(properties.getBaseUrl()))
                .defaultHeader("Authorization", "Bearer " + accessToken())
                .build();
    }

    private String accessToken() {
        AccessToken current = accessToken;
        Instant now = clock.instant();
        if (current != null && current.expiresAt().isAfter(now.plusSeconds(15))) {
            return current.value();
        }
        synchronized (this) {
            current = accessToken;
            if (current != null && current.expiresAt().isAfter(clock.instant().plusSeconds(15))) {
                return current.value();
            }
            if (properties.getClientId().isBlank() || properties.getClientSecret().isBlank()) {
                throw new KeycloakAdminException("Keycloak Admin service account is not configured");
            }
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "client_credentials");
            form.add("client_id", properties.getClientId());
            form.add("client_secret", properties.getClientSecret());
            try {
                TokenResponse response =
                        RestClient.builder()
                                .baseUrl(trimTrailingSlash(properties.getBaseUrl()))
                                .build()
                                .post()
                                .uri("/realms/{realm}/protocol/openid-connect/token", properties.getRealm())
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .body(form)
                                .retrieve()
                                .body(TokenResponse.class);
                if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
                    throw new KeycloakAdminException("Keycloak Admin service account returned no access token");
                }
                accessToken = new AccessToken(response.accessToken(), clock.instant().plusSeconds(response.expiresIn()));
                return accessToken.value();
            } catch (RestClientException exception) {
                throw translate("authenticate service account", null, null, exception);
            }
        }
    }

    private KeycloakAdminException translate(
            String operation, String keycloakUserId, String roleName, RestClientException exception) {
        int status =
                exception instanceof org.springframework.web.client.RestClientResponseException response
                        ? response.getStatusCode().value()
                        : -1;
        log.warn(
                "Keycloak Admin {} failed: status={}, keycloakUserId={}, role={}",
                operation,
                status,
                keycloakUserId,
                roleName);
        return new KeycloakAdminException("Keycloak Admin " + operation + " failed", exception);
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private record AccessToken(String value, Instant expiresAt) {}

    private record TokenResponse(String access_token, Integer expires_in) {
        String accessToken() {
            return access_token;
        }

        int expiresIn() {
            return expires_in == null ? 60 : Math.max(expires_in, 30);
        }
    }

    private record RoleRepresentation(String id, String name) {}
}
