package com.khanh.fooddelivery.delivery_service.security;

import com.khanh.fooddelivery.delivery_service.client.UserServiceClient;
import com.khanh.fooddelivery.delivery_service.client.dto.response.CurrentUserResponse;
import com.khanh.fooddelivery.delivery_service.exception.AppException;
import com.khanh.fooddelivery.delivery_service.exception.ErrorCode;
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
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        try {
            var response = userServiceClient.getCurrentUser("Bearer " + jwt.getTokenValue());
            CurrentUserResponse user = response == null || !response.success()
                    ? null
                    : response.data();
            if (user == null || user.id() == null) {
                throw new AppException(ErrorCode.DELIVERY_PROVIDER_UNAVAILABLE);
            }
            return user.id();
        } catch (FeignException exception) {
            throw new AppException(ErrorCode.DELIVERY_PROVIDER_UNAVAILABLE);
        }
    }
}
