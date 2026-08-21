package com.khanh.fooddelivery.notification_service.dto.response;

import com.khanh.fooddelivery.notification_service.entity.PushPlatform;
import java.time.Instant;
import java.util.UUID;

public record PushDeviceResponse(
        UUID id,
        PushPlatform platform,
        boolean active,
        Instant updatedAt
) {
}
