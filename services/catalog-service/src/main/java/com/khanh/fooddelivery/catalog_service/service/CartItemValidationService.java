package com.khanh.fooddelivery.catalog_service.service;

import com.khanh.fooddelivery.catalog_service.dto.request.internal.CartItemValidationRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.internal.CartItemValidationResponse;

public interface CartItemValidationService {
    CartItemValidationResponse validate(CartItemValidationRequest request);
}
