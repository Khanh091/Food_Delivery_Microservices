package com.khanh.fooddelivery.driver_service.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.driver_service.dto.request.DriverRegistrationRequest;
import com.khanh.fooddelivery.driver_service.model.VehicleType;
import com.khanh.fooddelivery.driver_service.security.CanonicalUserIdResolver;
import com.khanh.fooddelivery.driver_service.service.DriverService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class DriverAvailabilityControllerTests {

    private static final UUID KEYCLOAK_SUB =
            UUID.fromString("81e75fd2-9cc5-493e-bb2d-349ee7bbada9");
    private static final UUID CANONICAL_USER_ID =
            UUID.fromString("8d84b936-a0ce-41b2-ba44-62ca15058f77");

    @Mock
    private DriverService drivers;
    @Mock
    private CanonicalUserIdResolver currentUser;

    @Test
    void registration_passes_resolved_canonical_user_id_to_driver_service() {
        Jwt jwt = Jwt.withTokenValue("access-token")
                .header("alg", "none")
                .subject(KEYCLOAK_SUB.toString())
                .claim("user_id", KEYCLOAK_SUB.toString())
                .build();
        DriverRegistrationRequest request =
                new DriverRegistrationRequest(VehicleType.MOTORBIKE, "29C127836");
        when(currentUser.resolve(jwt)).thenReturn(CANONICAL_USER_ID);

        new DriverAvailabilityController(drivers, currentUser).register(jwt, request);

        verify(drivers).register(CANONICAL_USER_ID, request);
    }
}
