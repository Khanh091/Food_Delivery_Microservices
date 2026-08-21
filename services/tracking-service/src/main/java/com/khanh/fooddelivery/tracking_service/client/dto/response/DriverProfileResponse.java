package com.khanh.fooddelivery.tracking_service.client.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DriverProfileResponse(
        UUID id,
        Long version,
        UUID userId,
        String status
) {
}
