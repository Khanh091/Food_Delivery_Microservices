package com.khanh.fooddelivery.driver_service.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.driver_service.client.UserServiceClient;
import com.khanh.fooddelivery.driver_service.client.dto.response.ApiResponse;
import com.khanh.fooddelivery.driver_service.client.dto.response.CurrentUserResponse;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class CanonicalUserIdResolverTests {

    private static final UUID KEYCLOAK_SUB =
            UUID.fromString("81e75fd2-9cc5-493e-bb2d-349ee7bbada9");
    private static final UUID CANONICAL_USER_ID =
            UUID.fromString("8d84b936-a0ce-41b2-ba44-62ca15058f77");

    @Mock
    private UserServiceClient userServiceClient;

    @Test
    void resolves_canonical_users_id_from_user_service_instead_of_jwt_subject() {
        when(userServiceClient.getCurrentUser("Bearer access-token"))
                .thenReturn(new ApiResponse<>(
                        true,
                        "SUCCESS",
                        "ok",
                        new CurrentUserResponse(CANONICAL_USER_ID),
                        Instant.now()));

        Jwt jwt = Jwt.withTokenValue("access-token")
                .header("alg", "none")
                .subject(KEYCLOAK_SUB.toString())
                .build();

        assertThat(new CanonicalUserIdResolver(userServiceClient).resolve(jwt))
                .isEqualTo(CANONICAL_USER_ID)
                .isNotEqualTo(KEYCLOAK_SUB);
        verify(userServiceClient).getCurrentUser("Bearer access-token");
    }
}
