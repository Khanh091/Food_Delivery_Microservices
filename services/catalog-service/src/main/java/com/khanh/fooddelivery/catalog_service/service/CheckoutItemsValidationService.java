package com.khanh.fooddelivery.catalog_service.service;

import com.khanh.fooddelivery.catalog_service.dto.request.internal.CheckoutItemsValidationRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.internal.CheckoutItemsValidationResponse;

public interface CheckoutItemsValidationService {
    CheckoutItemsValidationResponse validate(CheckoutItemsValidationRequest request);
}
