package com.khanh.fooddelivery.driver_service.controller;

import com.khanh.fooddelivery.driver_service.dto.request.DriverAvailabilityRequest;
import com.khanh.fooddelivery.driver_service.dto.request.DriverRegistrationRequest;
import com.khanh.fooddelivery.driver_service.dto.request.DriverStatusUpdateRequest;
import com.khanh.fooddelivery.driver_service.dto.response.DriverAvailabilityResponse;
import com.khanh.fooddelivery.driver_service.dto.response.DriverProfileResponse;
import com.khanh.fooddelivery.driver_service.security.CanonicalUserIdResolver;
import com.khanh.fooddelivery.driver_service.security.InternalRequestAuthenticator;
import com.khanh.fooddelivery.driver_service.service.DriverService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    private final CanonicalUserIdResolver canonicalUserIdResolver;
    private final InternalRequestAuthenticator internalRequests;

    @PostMapping("/api/v1/drivers/me/profile")
    public DriverProfileResponse register(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody DriverRegistrationRequest request
    ) {
        return drivers.register(canonicalUserIdResolver.resolve(jwt), request);
    }

    @GetMapping("/api/v1/drivers/me/profile")
    public ResponseEntity<DriverProfileResponse> profile(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return drivers.profile(canonicalUserIdResolver.resolve(jwt))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/api/v1/admin/drivers/{driverId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public DriverProfileResponse setStatus(
            @PathVariable UUID driverId,
            @Valid @RequestBody DriverStatusUpdateRequest request
    ) {
        return drivers.setStatus(driverId, request);
    }

    @PutMapping("/api/v1/drivers/me/availability")
    @PreAuthorize("hasRole('DRIVER')")
    public DriverAvailabilityResponse setAvailability(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody DriverAvailabilityRequest request
    ) {
        return drivers.setAvailability(canonicalUserIdResolver.resolve(jwt), request);
    }

    @GetMapping("/api/v1/drivers/me/availability")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<DriverAvailabilityResponse> availability(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return drivers.availability(canonicalUserIdResolver.resolve(jwt))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/internal/v1/drivers/available")
    public List<UUID> available(
            @org.springframework.web.bind.annotation.RequestHeader(
                    value = org.springframework.http.HttpHeaders.AUTHORIZATION,
                    required = false) String authorization
    ) {
        internalRequests.authenticate(authorization);
        return drivers.available();
    }

    @GetMapping("/internal/v1/drivers/{driverId}/active")
    public boolean active(
            @PathVariable UUID driverId,
            @org.springframework.web.bind.annotation.RequestHeader(
                    value = org.springframework.http.HttpHeaders.AUTHORIZATION,
                    required = false) String authorization
    ) {
        internalRequests.authenticate(authorization);
        return drivers.active(driverId);
    }

    @PostMapping("/internal/v1/drivers/{driverId}/offers/{deliveryId}/reserve")
    public void reserveOffer(
            @PathVariable UUID driverId,
            @PathVariable UUID deliveryId,
            @org.springframework.web.bind.annotation.RequestHeader(
                    value = org.springframework.http.HttpHeaders.AUTHORIZATION,
                    required = false) String authorization
    ) {
        internalRequests.authenticate(authorization);
        drivers.reserveOffer(driverId, deliveryId);
    }

    @PostMapping("/internal/v1/drivers/{driverId}/offers/{deliveryId}/accept")
    public void acceptOffer(
            @PathVariable UUID driverId,
            @PathVariable UUID deliveryId,
            @org.springframework.web.bind.annotation.RequestHeader(
                    value = org.springframework.http.HttpHeaders.AUTHORIZATION,
                    required = false) String authorization
    ) {
        internalRequests.authenticate(authorization);
        drivers.acceptOffer(driverId, deliveryId);
    }

    @PostMapping("/internal/v1/drivers/{driverId}/offers/{deliveryId}/release")
    public void releaseOffer(
            @PathVariable UUID driverId,
            @PathVariable UUID deliveryId,
            @org.springframework.web.bind.annotation.RequestHeader(
                    value = org.springframework.http.HttpHeaders.AUTHORIZATION,
                    required = false) String authorization
    ) {
        internalRequests.authenticate(authorization);
        drivers.releaseOffer(driverId, deliveryId);
    }

    @PostMapping("/internal/v1/drivers/{driverId}/release/{deliveryId}")
    public void releaseDelivery(
            @PathVariable UUID driverId,
            @PathVariable UUID deliveryId,
            @org.springframework.web.bind.annotation.RequestHeader(
                    value = org.springframework.http.HttpHeaders.AUTHORIZATION,
                    required = false) String authorization
    ) {
        internalRequests.authenticate(authorization);
        drivers.releaseDelivery(driverId, deliveryId);
    }

}
