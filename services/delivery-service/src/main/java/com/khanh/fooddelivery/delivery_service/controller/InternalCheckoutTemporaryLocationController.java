package com.khanh.fooddelivery.delivery_service.controller;

import com.khanh.fooddelivery.delivery_service.common.response.ApiResponse;
import com.khanh.fooddelivery.delivery_service.dto.response.CheckoutTemporaryLocationResponse;
import com.khanh.fooddelivery.delivery_service.exception.AppException;
import com.khanh.fooddelivery.delivery_service.exception.ErrorCode;
import com.khanh.fooddelivery.delivery_service.service.CheckoutTemporaryLocationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/delivery/checkout-locations/branches/{branchId}/current")
@RequiredArgsConstructor
public class InternalCheckoutTemporaryLocationController {
    private final CheckoutTemporaryLocationService checkoutTemporaryLocationService;

    @GetMapping
    public ApiResponse<CheckoutTemporaryLocationResponse> current(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID branchId) {
        CheckoutTemporaryLocationResponse location = checkoutTemporaryLocationService.getCurrent(jwt, branchId)
                .orElseThrow(() -> new AppException(ErrorCode.CHECKOUT_LOCATION_NOT_FOUND));
        return ApiResponse.success("Checkout location loaded", location);
    }
}
