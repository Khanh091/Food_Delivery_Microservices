package com.khanh.fooddelivery.notification_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterPushDeviceRequest(
        @NotBlank @Size(max = 255) String expoPushToken,
        @NotBlank @Size(max = 16) String platform,
        @Size(max = 255) String deviceId
) {
}
