package com.khanh.fooddelivery.tracking_service.service;

import com.khanh.fooddelivery.tracking_service.dto.request.DriverLocationUpdateRequest;
import com.khanh.fooddelivery.tracking_service.dto.response.DriverLocationResponse;
import com.khanh.fooddelivery.tracking_service.dto.response.NearestDriverResponse;
import java.math.BigDecimal;
import java.util.List;

public interface DriverLocationService {
    DriverLocationResponse update(
            String authorization,
            DriverLocationUpdateRequest request
    );

    List<NearestDriverResponse> nearest(
            BigDecimal latitude,
            BigDecimal longitude,
            double radiusMeters,
            long limit
    );
}
