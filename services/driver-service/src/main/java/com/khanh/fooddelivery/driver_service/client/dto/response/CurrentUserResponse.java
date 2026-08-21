package com.khanh.fooddelivery.driver_service.client.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CurrentUserResponse(UUID id) {
}
