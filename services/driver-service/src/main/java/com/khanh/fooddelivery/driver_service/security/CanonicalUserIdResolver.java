package com.khanh.fooddelivery.driver_service.security;

import com.khanh.fooddelivery.driver_service.client.UserServiceClient;
import com.khanh.fooddelivery.driver_service.client.dto.response.ApiResponse;
import com.khanh.fooddelivery.driver_service.client.dto.response.CurrentUserResponse;
import feign.FeignException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CanonicalUserIdResolver {

    private final UserServiceClient userServiceClient;

    public UUID resolve(Jwt jwt) {
        if (jwt == null || jwt.getTokenValue() == null || jwt.getTokenValue().isBlank()) {
            throw new IllegalStateException("Authenticated user is required");
        }

        try {
            ApiResponse<CurrentUserResponse> response = userServiceClient
                    .getCurrentUser("Bearer " + jwt.getTokenValue());
            if (response == null
                    || !response.success()
                    || response.data() == null
                    || response.data().id() == null) {
                throw new IllegalStateException("Canonical user profile was not found");
            }
            return response.data().id();
        } catch (FeignException exception) {
            throw new IllegalStateException("Unable to resolve canonical user profile", exception);
        }
    }
}
