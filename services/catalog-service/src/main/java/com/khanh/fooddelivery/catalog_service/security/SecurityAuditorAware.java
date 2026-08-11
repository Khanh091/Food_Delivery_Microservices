package com.khanh.fooddelivery.catalog_service.security;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component("securityAuditorAware")
public class SecurityAuditorAware implements AuditorAware<UUID> {
    @Override
    public Optional<UUID> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(jwt.getSubject()));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
