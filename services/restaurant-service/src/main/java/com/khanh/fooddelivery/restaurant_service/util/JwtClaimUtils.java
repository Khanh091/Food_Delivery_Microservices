package com.khanh.fooddelivery.restaurant_service.util;

import com.khanh.fooddelivery.restaurant_service.exception.AppException;
import com.khanh.fooddelivery.restaurant_service.exception.ErrorCode;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class JwtClaimUtils {
    public String getSubject(Jwt jwt) {
        String s = normalize(jwt.getSubject());
        if (s == null) throw new AppException(ErrorCode.UNAUTHENTICATED);
        return s;
    }

    public String claim(Jwt jwt, String name) {
        return normalize(jwt.getClaimAsString(name));
    }

    private String normalize(String s) {
        if (s == null) return null;
        s = s.trim();
        return s.isEmpty() ? null : s;
    }
}
