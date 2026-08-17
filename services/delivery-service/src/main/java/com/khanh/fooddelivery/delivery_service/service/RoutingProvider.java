package com.khanh.fooddelivery.delivery_service.service;

import java.math.BigDecimal;

public interface RoutingProvider {
    Route calculateRoute(BigDecimal originLatitude, BigDecimal originLongitude, BigDecimal destinationLatitude, BigDecimal destinationLongitude);

    record Route(long distanceMeters, long durationSeconds) {}
}
