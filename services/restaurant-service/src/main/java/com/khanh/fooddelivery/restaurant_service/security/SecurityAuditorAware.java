package com.khanh.fooddelivery.restaurant_service.security;

import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component("securityAuditorAware")
public class SecurityAuditorAware implements AuditorAware<String> {
    public Optional<String> getCurrentAuditor() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a != null
                && a.isAuthenticated()
                && a.getPrincipal() instanceof Jwt jwt
                && jwt.getSubject() != null
                && !jwt.getSubject().isBlank()) return Optional.of(jwt.getSubject());
        return Optional.of("system");
    }
}
