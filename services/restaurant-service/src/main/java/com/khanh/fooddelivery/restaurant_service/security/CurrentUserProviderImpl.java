package com.khanh.fooddelivery.restaurant_service.security;

import com.khanh.fooddelivery.restaurant_service.client.UserServiceClient;
import com.khanh.fooddelivery.restaurant_service.exception.AppException;
import com.khanh.fooddelivery.restaurant_service.exception.ErrorCode;
import feign.FeignException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUserProviderImpl implements CurrentUserProvider {
    private final UserServiceClient userServiceClient;

    public UUID getCurrentUserId(Jwt jwt) {
        String claim = jwt.getClaimAsString("user_id");
        if (claim != null && !claim.isBlank()) return parse(claim);
        try {
            UserServiceClient.ApiResponse<UserServiceClient.CurrentUserResponse> body =
                    userServiceClient.getCurrentUser("Bearer " + jwt.getTokenValue());
            if (body == null || body.data() == null || body.data().id() == null)
                throw new AppException(
                        ErrorCode.UNAUTHENTICATED, "Unable to resolve internal user id");
            return body.data().id();
        } catch (FeignException e) {
            throw new AppException(
                    ErrorCode.UNAUTHENTICATED,
                    "Unable to resolve internal user id from user-service");
        }
    }

    private UUID parse(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.UNAUTHENTICATED, "Invalid user_id claim");
        }
    }
}
