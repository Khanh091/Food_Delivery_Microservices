package com.khanh.fooddelivery.delivery_service.service;

import com.khanh.fooddelivery.delivery_service.dto.request.ReverseGeocodeRequest;
import com.khanh.fooddelivery.delivery_service.dto.response.ReverseGeocodeResponse;

public interface ReverseGeocodingService {
    ReverseGeocodeResponse reverseGeocode(ReverseGeocodeRequest request);
}
