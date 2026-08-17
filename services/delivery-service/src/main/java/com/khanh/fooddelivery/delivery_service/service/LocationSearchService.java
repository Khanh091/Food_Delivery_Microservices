package com.khanh.fooddelivery.delivery_service.service;

import com.khanh.fooddelivery.delivery_service.dto.response.LocationSearchCandidateResponse;
import com.khanh.fooddelivery.delivery_service.dto.response.ReverseGeocodeResponse;
import java.math.BigDecimal;
import java.util.List;

public interface LocationSearchService {
    List<LocationSearchCandidateResponse> search(String query, BigDecimal latitude, BigDecimal longitude, Integer limit);

    ReverseGeocodeResponse place(String providerRefId);
}
