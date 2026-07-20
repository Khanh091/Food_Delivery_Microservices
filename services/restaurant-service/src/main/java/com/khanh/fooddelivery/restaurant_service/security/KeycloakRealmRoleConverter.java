package com.khanh.fooddelivery.restaurant_service.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Object realm = jwt.getClaim("realm_access");
        if (!(realm instanceof Map<?, ?> map) || !(map.get("roles") instanceof Collection<?> roles))
            return List.of();
        return roles.stream()
                .map(Object::toString)
                .map(String::toUpperCase)
                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r))
                .toList();
    }
}
