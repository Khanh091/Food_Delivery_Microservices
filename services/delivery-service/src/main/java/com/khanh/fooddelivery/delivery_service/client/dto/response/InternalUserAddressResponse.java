package com.khanh.fooddelivery.delivery_service.client.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InternalUserAddressResponse(UUID id, BigDecimal latitude, BigDecimal longitude) {
}
