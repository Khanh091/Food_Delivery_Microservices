package com.khanh.fooddelivery.payment_service.security;

import com.khanh.fooddelivery.payment_service.config.InternalApiProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class InternalRequestAuthenticator {
    private final InternalApiProperties properties;

    public void authenticate(String supplied) {
        byte[] expected = value(properties.getKey());
        byte[] actual = value(supplied);
        if (expected.length == 0 || !MessageDigest.isEqual(expected, actual)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal API key");
        }
    }

    private byte[] value(String value) {
        return (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
    }
}
