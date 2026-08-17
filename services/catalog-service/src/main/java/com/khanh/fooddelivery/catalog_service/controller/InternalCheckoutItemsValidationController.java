package com.khanh.fooddelivery.catalog_service.controller;

import com.khanh.fooddelivery.catalog_service.common.response.ApiResponse;
import com.khanh.fooddelivery.catalog_service.dto.request.internal.CheckoutItemsValidationRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.internal.CheckoutItemsValidationResponse;
import com.khanh.fooddelivery.catalog_service.service.CheckoutItemsValidationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/catalog/checkout-items")
@RequiredArgsConstructor
public class InternalCheckoutItemsValidationController {
    private final CheckoutItemsValidationService checkoutItemsValidationService;

    @PostMapping("/validate")
    public ApiResponse<CheckoutItemsValidationResponse> validate(
            @Valid @RequestBody CheckoutItemsValidationRequest request) {
        return ApiResponse.success("Checkout items validated", checkoutItemsValidationService.validate(request));
    }
}
