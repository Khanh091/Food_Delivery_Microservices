package com.khanh.fooddelivery.driver_service.controller;

import com.khanh.fooddelivery.driver_service.dto.request.DriverAvailabilityRequest;
import com.khanh.fooddelivery.driver_service.dto.request.DriverRegistrationRequest;
import com.khanh.fooddelivery.driver_service.dto.request.DriverStatusUpdateRequest;
import com.khanh.fooddelivery.driver_service.dto.response.DriverAvailabilityResponse;
import com.khanh.fooddelivery.driver_service.dto.response.DriverProfileResponse;
import com.khanh.fooddelivery.driver_service.service.DriverService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DriverAvailabilityController {

    private final DriverService drivers;

    @PostMapping("/api/v1/drivers/me/profile")
    public DriverProfileResponse register(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody DriverRegistrationRequest request
    ) {
        return drivers.register(user(jwt), request);
    }

    @PutMapping("/api/v1/admin/drivers/{driverId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public DriverProfileResponse setStatus(
            @PathVariable UUID driverId,
            @RequestBody DriverStatusUpdateRequest request
    ) {
        return drivers.setStatus(driverId, request);
    }

    @PutMapping("/api/v1/drivers/me/availability")
    @PreAuthorize("hasRole('DRIVER')")
    public DriverAvailabilityResponse setAvailability(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody DriverAvailabilityRequest request
    ) {
        return drivers.setAvailability(user(jwt), request);
    }

    @GetMapping("/internal/v1/drivers/available")
    public List<UUID> available() {
        return drivers.available();
    }

    @GetMapping("/internal/v1/drivers/{driverId}/active")
    public boolean active(@PathVariable UUID driverId) {
        return drivers.active(driverId);
    }

    @PostMapping("/internal/v1/drivers/{driverId}/offers/{deliveryId}/reserve")
    public void reserveOffer(
            @PathVariable UUID driverId,
            @PathVariable UUID deliveryId
    ) {
        drivers.reserveOffer(driverId, deliveryId);
    }

    @PostMapping("/internal/v1/drivers/{driverId}/offers/{deliveryId}/accept")
    public void acceptOffer(
            @PathVariable UUID driverId,
            @PathVariable UUID deliveryId
    ) {
        drivers.acceptOffer(driverId, deliveryId);
    }

    @PostMapping("/internal/v1/drivers/{driverId}/offers/{deliveryId}/release")
    public void releaseOffer(
            @PathVariable UUID driverId,
            @PathVariable UUID deliveryId
    ) {
        drivers.releaseOffer(driverId, deliveryId);
    }

    @PostMapping("/internal/v1/drivers/{driverId}/release/{deliveryId}")
    public void releaseDelivery(
            @PathVariable UUID driverId,
            @PathVariable UUID deliveryId
    ) {
        drivers.releaseDelivery(driverId, deliveryId);
    }

    private UUID user(Jwt jwt) {
        String value = jwt.getClaimAsString("user_id");
        return UUID.fromString(value == null ? jwt.getSubject() : value);
    }
}
