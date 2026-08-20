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
        String claim = jwt.getClaimAsString("user_id");
        if (claim != null && !claim.isBlank()) return parse(claim);
        try {
            CurrentUserResponse user = userServiceClient.getCurrentUser("Bearer " + jwt.getTokenValue()).data();
            if (user == null || user.id() == null) throw new AppException(ErrorCode.DELIVERY_PROVIDER_UNAVAILABLE);
            return user.id();
        } catch (FeignException exception) {
            throw new AppException(ErrorCode.DELIVERY_PROVIDER_UNAVAILABLE);
        }
    }

    private UUID parse(String value) {
        try { return UUID.fromString(value); }
        catch (IllegalArgumentException exception) { throw new AppException(ErrorCode.UNAUTHENTICATED, "Invalid user_id claim"); }
    }
}
