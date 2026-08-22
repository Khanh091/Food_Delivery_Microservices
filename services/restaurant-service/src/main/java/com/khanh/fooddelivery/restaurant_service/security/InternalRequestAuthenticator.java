package com.khanh.fooddelivery.restaurant_service.security;

import com.khanh.fooddelivery.restaurant_service.exception.AppException;
import com.khanh.fooddelivery.restaurant_service.exception.ErrorCode;
import com.khanh.fooddelivery.restaurant_service.config.InternalApiProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InternalRequestAuthenticator {

    private final InternalApiProperties internalApi;

    public void authenticate(String suppliedKey, Jwt jwt) {
        byte[] expected = value(internalApi.getKey());
        byte[] supplied = value(suppliedKey);
        if (expected.length > 0 && MessageDigest.isEqual(expected, supplied)) {
            return;
        }
        if (jwt != null) {
            return;
        }
        throw new AppException(ErrorCode.ACCESS_DENIED, "Invalid internal service credential");
    }

    private byte[] value(String value) {
        return (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
    }
}
