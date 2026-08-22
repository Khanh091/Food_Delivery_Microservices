package com.khanh.fooddelivery.delivery_service.security;

import com.khanh.fooddelivery.delivery_service.exception.AppException;
import com.khanh.fooddelivery.delivery_service.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InternalRequestAuthenticator {

    @Value("${app.internal-api.key:}")
    private String internalApiKey;

    public void authenticate(String suppliedKey) {
        byte[] expected = value(internalApiKey);
        byte[] supplied = value(suppliedKey);
        if (expected.length == 0 || !MessageDigest.isEqual(expected, supplied)) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "Invalid internal service credential");
        }
    }

    private byte[] value(String value) {
        return (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
    }
}
