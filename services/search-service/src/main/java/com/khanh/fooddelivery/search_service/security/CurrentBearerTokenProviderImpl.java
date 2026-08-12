package com.khanh.fooddelivery.search_service.security;

import com.khanh.fooddelivery.search_service.exception.SearchApiException;
import com.khanh.fooddelivery.search_service.exception.SearchErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class CurrentBearerTokenProviderImpl implements CurrentBearerTokenProvider {
    @Override
    public String getBearerToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new SearchApiException(SearchErrorCode.UNAUTHENTICATED);
        }
        return "Bearer " + jwt.getTokenValue();
    }
}
