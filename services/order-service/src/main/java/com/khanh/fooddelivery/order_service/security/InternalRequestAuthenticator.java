package com.khanh.fooddelivery.order_service.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class InternalRequestAuthenticator {

    private static final String INTERNAL_PREFIX = "Internal ";

    @Value("${app.internal-api.key:}")
    private String internalApiKey;

    public void authenticate(String authorization) {
        if (authorization != null && authorization.startsWith(INTERNAL_PREFIX)) {
            byte[] expected = bytes(internalApiKey);
            byte[] supplied = bytes(authorization.substring(INTERNAL_PREFIX.length()));
            if (expected.length > 0 && MessageDigest.isEqual(expected, supplied)) {
                return;
            }
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof Jwt) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal service credential");
    }

    private byte[] bytes(String value) {
        return (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
    }
}
