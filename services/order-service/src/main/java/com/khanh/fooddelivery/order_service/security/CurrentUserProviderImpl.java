package com.khanh.fooddelivery.order_service.security;

import com.khanh.fooddelivery.order_service.client.UserServiceClient;
import com.khanh.fooddelivery.order_service.client.dto.response.CurrentUserResponse;
import com.khanh.fooddelivery.order_service.exception.AppException;
import com.khanh.fooddelivery.order_service.exception.ErrorCode;
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
            var response = userServiceClient
                    .getCurrentUser("Bearer " + jwt.getTokenValue());
            CurrentUserResponse current = response == null || !response.success()
                    ? null
                    : response.data();
            if (current == null || current.id() == null) throw new AppException(ErrorCode.USER_SERVICE_UNAVAILABLE);
            return current.id();
        } catch (FeignException exception) {
            throw new AppException(ErrorCode.USER_SERVICE_UNAVAILABLE);
        }
    }
}
