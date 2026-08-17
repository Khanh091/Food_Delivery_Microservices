package com.khanh.fooddelivery.delivery_service.controller;

import com.khanh.fooddelivery.delivery_service.common.response.ApiResponse;
import com.khanh.fooddelivery.delivery_service.dto.request.UpsertCheckoutTemporaryLocationRequest;
import com.khanh.fooddelivery.delivery_service.dto.response.CheckoutTemporaryLocationResponse;
import com.khanh.fooddelivery.delivery_service.service.CheckoutTemporaryLocationService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/delivery/checkout-locations/branches/{branchId}/current")
@RequiredArgsConstructor
public class CheckoutTemporaryLocationController {
    private final CheckoutTemporaryLocationService checkoutTemporaryLocationService;

    @PutMapping
    public ApiResponse<CheckoutTemporaryLocationResponse> upsert(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID branchId,
            @Valid @RequestBody UpsertCheckoutTemporaryLocationRequest request) {
        return ApiResponse.success("Checkout location saved", checkoutTemporaryLocationService.upsert(jwt, branchId, request));
    }

    @GetMapping
    public ApiResponse<CheckoutTemporaryLocationResponse> current(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID branchId) {
        CheckoutTemporaryLocationResponse location = checkoutTemporaryLocationService.getCurrent(jwt, branchId).orElse(null);
        return ApiResponse.success("Checkout location loaded", location);
    }
}
