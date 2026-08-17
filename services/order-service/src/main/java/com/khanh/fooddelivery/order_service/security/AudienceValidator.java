package com.khanh.fooddelivery.order_service.security;

import java.util.List;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class AudienceValidator implements OAuth2TokenValidator<Jwt> {
    private final String audience;
    private final OAuth2Error error = new OAuth2Error("invalid_token", "Required audience is missing", null);

    public AudienceValidator(String audience) { this.audience = audience; }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        List<String> audiences = jwt.getAudience();
        return audiences.contains(audience) ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(error);
    }
}
