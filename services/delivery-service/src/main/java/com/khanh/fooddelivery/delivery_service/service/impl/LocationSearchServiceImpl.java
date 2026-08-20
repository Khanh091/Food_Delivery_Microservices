package com.khanh.fooddelivery.delivery_service.service.impl;

import com.khanh.fooddelivery.delivery_service.dto.response.LocationSearchCandidateResponse;
import com.khanh.fooddelivery.delivery_service.dto.response.ReverseGeocodeResponse;
import com.khanh.fooddelivery.delivery_service.exception.AppException;
import com.khanh.fooddelivery.delivery_service.exception.ErrorCode;
import com.khanh.fooddelivery.delivery_service.mapper.GeocodingMapper;
import com.khanh.fooddelivery.delivery_service.service.GeocodingProvider;
import com.khanh.fooddelivery.delivery_service.service.LocationSearchService;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocationSearchServiceImpl implements LocationSearchService {
    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 10;
    private final GeocodingProvider geocodingProvider;
    private final GeocodingMapper geocodingMapper;

    @Override
    public List<LocationSearchCandidateResponse> search(String query, BigDecimal latitude, BigDecimal longitude, Integer limit) {
        if (query == null || query.trim().length() < 3 || (latitude == null) != (longitude == null)
                || invalid(latitude, -90, 90) || invalid(longitude, -180, 180)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        int resolvedLimit = limit == null ? DEFAULT_LIMIT : Math.min(Math.max(limit, 1), MAX_LIMIT);
        return geocodingProvider.search(query.trim(), latitude, longitude, resolvedLimit).stream()
                .map(geocodingMapper::toSearchResponse)
                .toList();
    }

    private boolean invalid(BigDecimal value, int min, int max) {
        return value != null && (value.compareTo(BigDecimal.valueOf(min)) < 0 || value.compareTo(BigDecimal.valueOf(max)) > 0);
    }

    @Override
    public ReverseGeocodeResponse place(String providerRefId) {
        GeocodingProvider.GeocodedLocation location = geocodingProvider.place(providerRefId);
        if (location == null || location.latitude() == null || location.longitude() == null) throw new AppException(ErrorCode.LOCATION_NOT_FOUND);
        return geocodingMapper.toReverseResponse(location);
    }
}
