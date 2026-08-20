package com.khanh.fooddelivery.tracking_service.controller;

import com.khanh.fooddelivery.tracking_service.dto.request.DriverLocationUpdateRequest;
import com.khanh.fooddelivery.tracking_service.dto.response.DriverLocationResponse;
import com.khanh.fooddelivery.tracking_service.dto.response.NearestDriverResponse;
import com.khanh.fooddelivery.tracking_service.service.DriverLocationService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DriverLocationController {

    private final DriverLocationService locations;

    @PutMapping("/api/v1/tracking/drivers/me/location")
    @PreAuthorize("hasRole('DRIVER')")
    public DriverLocationResponse update(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody DriverLocationUpdateRequest request
    ) {
        UUID driverId = user(jwt);

        return locations.update(driverId, "Bearer " + jwt.getTokenValue(), request);
    }

    @GetMapping("/internal/v1/tracking/drivers/nearest")
    public List<NearestDriverResponse> nearest(
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude,
            @RequestParam double radiusMeters,
            @RequestParam(defaultValue = "20") long limit
    ) {
        return locations.nearest(latitude, longitude, radiusMeters, limit);
    }

    private UUID user(Jwt jwt) {
        String value = jwt.getClaimAsString("user_id");

        return UUID.fromString(
                value == null
                        ? jwt.getSubject()
                        : value
        );
    }

}
