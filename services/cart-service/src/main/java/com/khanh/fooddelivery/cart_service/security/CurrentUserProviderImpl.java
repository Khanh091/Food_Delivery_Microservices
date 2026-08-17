package com.khanh.fooddelivery.cart_service.security;

import com.khanh.fooddelivery.cart_service.client.UserServiceClient;
import com.khanh.fooddelivery.cart_service.exception.AppException;
import com.khanh.fooddelivery.cart_service.exception.ErrorCode;
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
        String claim = jwt.getClaimAsString("user_id");
        if (claim != null && !claim.isBlank()) return parse(claim);
        try {
            UserServiceClient.ApiResponse<UserServiceClient.CurrentUserResponse> response =
                    userServiceClient.getCurrentUser("Bearer " + jwt.getTokenValue());
            if (response == null || response.data() == null || response.data().id() == null) {
                throw new AppException(ErrorCode.USER_SERVICE_UNAVAILABLE);
            }
            return response.data().id();
        } catch (FeignException exception) {
            throw new AppException(ErrorCode.USER_SERVICE_UNAVAILABLE);
        }
    }

    private UUID parse(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new AppException(ErrorCode.UNAUTHENTICATED, "Invalid user_id claim");
        }
    }
}
