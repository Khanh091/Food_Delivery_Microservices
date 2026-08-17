package com.khanh.fooddelivery.delivery_service.service;

import com.khanh.fooddelivery.delivery_service.dto.request.UpsertCheckoutTemporaryLocationRequest;
import com.khanh.fooddelivery.delivery_service.dto.response.CheckoutTemporaryLocationResponse;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

public interface CheckoutTemporaryLocationService {
    CheckoutTemporaryLocationResponse upsert(Jwt jwt, UUID branchId, UpsertCheckoutTemporaryLocationRequest request);
    Optional<CheckoutTemporaryLocationResponse> getCurrent(Jwt jwt, UUID branchId);
}
