package com.khanh.fooddelivery.order_service.client.dto.response;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.UUID;
@JsonIgnoreProperties(ignoreUnknown = true)
public record InternalUserAddressResponse(
        UUID id,
        String labelType,
        String customLabel,
        String displayLabel,
        String recipientName,
        String recipientPhone,
        String addressLine,
        String ward,
        String district,
        String city,
        BigDecimal latitude,
        BigDecimal longitude,
        String buildingName,
        String floor,
        String entrance,
        String deliveryNote,
        Long version
) {
}
