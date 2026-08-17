package com.khanh.fooddelivery.delivery_service.service;

import com.khanh.fooddelivery.delivery_service.dto.request.CreateDeliveryQuoteRequest;
import com.khanh.fooddelivery.delivery_service.dto.response.DeliveryQuoteResponse;
import org.springframework.security.oauth2.jwt.Jwt;

public interface DeliveryQuoteService {
    DeliveryQuoteResponse createQuote(Jwt jwt, CreateDeliveryQuoteRequest request);
}
