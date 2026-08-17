package com.khanh.fooddelivery.delivery_service.service;

import java.math.BigDecimal;

public interface GeocodingProvider {
    GeocodedLocation reverseGeocode(BigDecimal latitude, BigDecimal longitude);

    java.util.List<GeocodedLocation> search(String query, BigDecimal latitude, BigDecimal longitude, int limit);

    GeocodedLocation place(String providerRefId);

    record GeocodedLocation(
            String providerRefId,
            String formattedAddress,
            String addressLine,
            String ward,
            String district,
            String city,
            BigDecimal latitude,
            BigDecimal longitude) {}
}
