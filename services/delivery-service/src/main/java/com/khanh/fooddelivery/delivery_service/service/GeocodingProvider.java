package com.khanh.fooddelivery.delivery_service.service;

import java.math.BigDecimal;

public interface GeocodingProvider {
    GeocodedLocation reverseGeocode(BigDecimal latitude, BigDecimal longitude);

    record GeocodedLocation(
            String formattedAddress,
            String addressLine,
            String ward,
            String district,
            String city,
            BigDecimal latitude,
            BigDecimal longitude) {}
}
