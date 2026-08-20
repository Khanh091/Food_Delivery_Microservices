package com.khanh.fooddelivery.delivery_service.service.impl;

import com.khanh.fooddelivery.delivery_service.dto.request.ReverseGeocodeRequest;
import com.khanh.fooddelivery.delivery_service.dto.response.ReverseGeocodeResponse;
import com.khanh.fooddelivery.delivery_service.exception.AppException;
import com.khanh.fooddelivery.delivery_service.exception.ErrorCode;
import com.khanh.fooddelivery.delivery_service.mapper.GeocodingMapper;
import com.khanh.fooddelivery.delivery_service.service.GeocodingProvider;
import com.khanh.fooddelivery.delivery_service.service.ReverseGeocodingService;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReverseGeocodingServiceImpl implements ReverseGeocodingService {
    private final GeocodingProvider geocodingProvider;
    private final GeocodingMapper geocodingMapper;

    @Override
    public ReverseGeocodeResponse reverseGeocode(ReverseGeocodeRequest request) {
        if (request == null || outside(request.latitude(), -90, 90) || outside(request.longitude(), -180, 180)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        GeocodingProvider.GeocodedLocation location = geocodingProvider.reverseGeocode(request.latitude(), request.longitude());
        if (location == null || blank(location.formattedAddress())) throw new AppException(ErrorCode.LOCATION_NOT_FOUND);
        return geocodingMapper.toReverseResponse(location, request.latitude(), request.longitude());
    }

    private boolean outside(BigDecimal value, int min, int max) {
        return value == null || value.compareTo(BigDecimal.valueOf(min)) < 0 || value.compareTo(BigDecimal.valueOf(max)) > 0;
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
}
