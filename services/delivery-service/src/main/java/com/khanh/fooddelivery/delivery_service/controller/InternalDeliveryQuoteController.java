package com.khanh.fooddelivery.delivery_service.controller;

import com.khanh.fooddelivery.delivery_service.common.response.ApiResponse;
import com.khanh.fooddelivery.delivery_service.dto.request.CreateDeliveryQuoteRequest;
import com.khanh.fooddelivery.delivery_service.dto.response.DeliveryQuoteResponse;
import com.khanh.fooddelivery.delivery_service.service.DeliveryQuoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/delivery/quotes")
@RequiredArgsConstructor
public class InternalDeliveryQuoteController {
    private final DeliveryQuoteService deliveryQuoteService;

    @PostMapping
    public ApiResponse<DeliveryQuoteResponse> createQuote(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateDeliveryQuoteRequest request) {
        return ApiResponse.success("Delivery quote calculated", deliveryQuoteService.createQuote(jwt, request));
    }
}
