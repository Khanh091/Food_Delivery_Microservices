package com.khanh.fooddelivery.payment_service.security;

import com.khanh.fooddelivery.payment_service.client.UserServiceClient;
import com.khanh.fooddelivery.payment_service.client.dto.response.CurrentUserResponse;
import com.khanh.fooddelivery.payment_service.exception.PaymentException;
import feign.FeignException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUserProviderImpl implements CurrentUserProvider {
    private final UserServiceClient userServiceClient;

    @Override
    public UUID getCurrentUserId(Jwt jwt) {
        if (jwt == null || jwt.getTokenValue() == null || jwt.getTokenValue().isBlank()) {
            throw new PaymentException("PAYMENT_401", org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "Authentication is required");
        }
        try {
            var response = userServiceClient.getCurrentUser("Bearer " + jwt.getTokenValue());
            CurrentUserResponse current = response == null || !response.success() ? null : response.data();
            if (current == null || current.id() == null) {
                throw new PaymentException("PAYMENT_503", org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                        "Canonical user identity is unavailable");
            }
            return current.id();
        } catch (FeignException exception) {
            throw new PaymentException("PAYMENT_503", org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "Canonical user identity is unavailable");
        }
    }
}
