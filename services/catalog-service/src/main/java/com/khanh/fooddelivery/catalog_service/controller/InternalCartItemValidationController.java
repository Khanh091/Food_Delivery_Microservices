package com.khanh.fooddelivery.catalog_service.controller;

import com.khanh.fooddelivery.catalog_service.common.response.ApiResponse;
import com.khanh.fooddelivery.catalog_service.dto.request.internal.CartItemValidationRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.internal.CartItemValidationResponse;
import com.khanh.fooddelivery.catalog_service.service.CartItemValidationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/catalog/cart-items")
@RequiredArgsConstructor
public class InternalCartItemValidationController {
    private final CartItemValidationService cartItemValidationService;

    @PostMapping("/validate")
    public ApiResponse<CartItemValidationResponse> validate(
            @Valid @RequestBody CartItemValidationRequest request) {
        return ApiResponse.success("Cart item validated", cartItemValidationService.validate(request));
    }
}
