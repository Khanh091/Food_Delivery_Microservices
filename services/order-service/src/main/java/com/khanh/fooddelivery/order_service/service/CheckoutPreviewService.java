package com.khanh.fooddelivery.order_service.service;

import com.khanh.fooddelivery.order_service.dto.request.CheckoutPreviewRequest;
import com.khanh.fooddelivery.order_service.dto.response.CheckoutPreviewResponse;
import org.springframework.security.oauth2.jwt.Jwt;

public interface CheckoutPreviewService {
    CheckoutPreviewResponse preview(Jwt jwt, CheckoutPreviewRequest request);
}
