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
        String claim = jwt.getClaimAsString("user_id");
        if (claim != null && !claim.isBlank()) return parse(claim);
        try {
            CurrentUserResponse current = userServiceClient
                    .getCurrentUser("Bearer " + jwt.getTokenValue()).data();
            if (current == null || current.id() == null) throw new AppException(ErrorCode.USER_SERVICE_UNAVAILABLE);
            return current.id();
        } catch (FeignException exception) {
            throw new AppException(ErrorCode.USER_SERVICE_UNAVAILABLE);
        }
    }

    private UUID parse(String value) {
        try { return UUID.fromString(value); }
        catch (IllegalArgumentException exception) { throw new AppException(ErrorCode.UNAUTHENTICATED, "Invalid user_id claim"); }
    }
}
