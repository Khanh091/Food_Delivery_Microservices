package com.khanh.fooddelivery.notification_service.controller;

import com.khanh.fooddelivery.notification_service.common.response.ApiResponse;
import com.khanh.fooddelivery.notification_service.dto.request.RegisterPushDeviceRequest;
import com.khanh.fooddelivery.notification_service.dto.response.PushDeviceResponse;
import com.khanh.fooddelivery.notification_service.service.NotificationService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notifications;

    @PostMapping("/api/v1/notifications/devices")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<PushDeviceResponse> register(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RegisterPushDeviceRequest request
    ) {
        return ApiResponse.success("Push device registered", notifications.registerDevice(user(jwt), request));
    }

    @DeleteMapping("/api/v1/notifications/devices/{deviceId}")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<Void> deactivate(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID deviceId
    ) {
        notifications.deactivateDevice(user(jwt), deviceId);
        return ApiResponse.success("Push device deactivated", null);
    }

    private UUID user(Jwt jwt) {
        String value = jwt.getClaimAsString("user_id");
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Canonical user identity is missing"
            );
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user identity");
        }
    }
}
