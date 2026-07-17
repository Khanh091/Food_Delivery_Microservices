package com.khanh.fooddelivery.api_gateway.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
public class KeycloakRealmRoleConverter
        implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String REALM_ACCESS = "realm_access";
    private static final String ROLES = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> realmAccess =
                jwt.getClaimAsMap(REALM_ACCESS);

        if (realmAccess == null) {
            return List.of();
        }

        Object rolesObject = realmAccess.get(ROLES);

        if (!(rolesObject instanceof Collection<?> roles)) {
            return List.of();
        }

        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(String::toUpperCase)
                .<GrantedAuthority>map(role -> new SimpleGrantedAuthority(
                        ROLE_PREFIX + role
                ))
                .toList();
    }
}
