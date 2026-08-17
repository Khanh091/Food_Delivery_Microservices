package com.khanh.fooddelivery.delivery_service.dto.response;

import java.math.BigDecimal;

public record ReverseGeocodeResponse(
        String formattedAddress,
        String addressLine,
        String ward,
        String district,
        String city,
        BigDecimal latitude,
        BigDecimal longitude) {}
