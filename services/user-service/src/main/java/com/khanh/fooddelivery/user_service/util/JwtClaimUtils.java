package com.khanh.fooddelivery.user_service.util;

import com.khanh.fooddelivery.user_service.exception.AppException;
import com.khanh.fooddelivery.user_service.exception.ErrorCode;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class JwtClaimUtils {

    public String getSubject(Jwt jwt) {
        String subject = normalize(jwt.getSubject());
        if (subject == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return subject;
    }

    public String getUsername(Jwt jwt) {
        return claim(jwt, "preferred_username");
    }

    public String getEmail(Jwt jwt) {
        return claim(jwt, "email");
    }

    public String getPhoneNumber(Jwt jwt) {
        return claim(jwt, "phone_number");
    }

    public String getFullName(Jwt jwt) {
        String name = claim(jwt, "name");
        if (name != null) {
            return name;
        }

        String combinedName = Stream.of(
                        claim(jwt, "given_name"),
                        claim(jwt, "family_name")
                )
                .filter(value -> value != null)
                .collect(Collectors.joining(" "));
        return normalize(combinedName);
    }

    private String claim(Jwt jwt, String claimName) {
        return normalize(jwt.getClaimAsString(claimName));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
