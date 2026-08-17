package com.khanh.fooddelivery.user_service.dto.response.internal;

import com.khanh.fooddelivery.user_service.enums.AddressLabelType;
import java.math.BigDecimal;
import java.util.UUID;

public record InternalUserAddressResponse(
        UUID id,
        AddressLabelType labelType,
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
        Long version) {}
