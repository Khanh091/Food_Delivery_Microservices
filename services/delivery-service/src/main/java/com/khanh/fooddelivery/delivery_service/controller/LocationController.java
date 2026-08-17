package com.khanh.fooddelivery.delivery_service.controller;

import com.khanh.fooddelivery.delivery_service.common.response.ApiResponse;
import com.khanh.fooddelivery.delivery_service.dto.request.ReverseGeocodeRequest;
import com.khanh.fooddelivery.delivery_service.dto.response.ReverseGeocodeResponse;
import com.khanh.fooddelivery.delivery_service.dto.response.LocationSearchCandidateResponse;
import com.khanh.fooddelivery.delivery_service.service.LocationSearchService;
import com.khanh.fooddelivery.delivery_service.service.ReverseGeocodingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/delivery/locations")
@RequiredArgsConstructor
public class LocationController {
    private final ReverseGeocodingService reverseGeocodingService;
    private final LocationSearchService locationSearchService;

    @PostMapping("/reverse-geocode")
    public ApiResponse<ReverseGeocodeResponse> reverseGeocode(@Valid @RequestBody ReverseGeocodeRequest request) {
        return ApiResponse.success("Location candidate resolved", reverseGeocodingService.reverseGeocode(request));
    }

    @org.springframework.web.bind.annotation.GetMapping("/search")
    public ApiResponse<java.util.List<LocationSearchCandidateResponse>> search(
            @RequestParam String query,
            @RequestParam(required = false) java.math.BigDecimal latitude,
            @RequestParam(required = false) java.math.BigDecimal longitude,
            @RequestParam(required = false) Integer limit) {
        return ApiResponse.success("Location candidates resolved", locationSearchService.search(query, latitude, longitude, limit));
    }

    @GetMapping("/place")
    public ApiResponse<ReverseGeocodeResponse> place(@RequestParam String providerRefId) {
        return ApiResponse.success("Location candidate resolved", locationSearchService.place(providerRefId));
    }
}
