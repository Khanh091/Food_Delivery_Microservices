package com.khanh.fooddelivery.order_service.security;

import com.khanh.fooddelivery.order_service.exception.AppException;
import com.khanh.fooddelivery.order_service.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class CurrentBearerTokenProviderImpl implements CurrentBearerTokenProvider {
    @Override
    public String getBearerToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return "Bearer " + jwt.getTokenValue();
    }
}
