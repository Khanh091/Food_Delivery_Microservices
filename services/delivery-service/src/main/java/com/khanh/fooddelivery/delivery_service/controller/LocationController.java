package com.khanh.fooddelivery.delivery_service.controller;

import com.khanh.fooddelivery.delivery_service.common.response.ApiResponse;
import com.khanh.fooddelivery.delivery_service.dto.request.ReverseGeocodeRequest;
import com.khanh.fooddelivery.delivery_service.dto.response.ReverseGeocodeResponse;
import com.khanh.fooddelivery.delivery_service.service.ReverseGeocodingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/delivery/locations")
@RequiredArgsConstructor
public class LocationController {
    private final ReverseGeocodingService reverseGeocodingService;

    @PostMapping("/reverse-geocode")
    public ApiResponse<ReverseGeocodeResponse> reverseGeocode(@Valid @RequestBody ReverseGeocodeRequest request) {
        return ApiResponse.success("Location candidate resolved", reverseGeocodingService.reverseGeocode(request));
    }
}
