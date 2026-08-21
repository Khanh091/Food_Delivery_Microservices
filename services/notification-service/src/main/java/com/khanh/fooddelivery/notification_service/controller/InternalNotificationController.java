package com.khanh.fooddelivery.notification_service.controller;

import com.khanh.fooddelivery.notification_service.common.response.ApiResponse;
import com.khanh.fooddelivery.notification_service.config.InternalApiProperties;
import com.khanh.fooddelivery.notification_service.dto.request.DriverOfferNotificationRequest;
import com.khanh.fooddelivery.notification_service.service.NotificationService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequiredArgsConstructor
public class InternalNotificationController {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final NotificationService notifications;
    private final InternalApiProperties internalApi;

    @PostMapping("/internal/v1/notifications/driver-offers")
    public ApiResponse<Void> driverOffer(
            @RequestHeader(name = INTERNAL_API_KEY_HEADER, required = false) String apiKey,
            @Valid @RequestBody DriverOfferNotificationRequest request
    ) {
        authenticate(apiKey);
        notifications.notifyDriverOffer(request);
        return ApiResponse.success("Driver offer notification requested", null);
    }

    private void authenticate(String apiKey) {
        byte[] expected = internalApi.getKey().getBytes(StandardCharsets.UTF_8);
        byte[] supplied = (apiKey == null ? "" : apiKey).getBytes(StandardCharsets.UTF_8);
        if (expected.length == 0 || !MessageDigest.isEqual(expected, supplied)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal API key");
        }
    }
}
