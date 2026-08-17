package com.khanh.fooddelivery.order_service.controller;

import com.khanh.fooddelivery.order_service.common.response.ApiResponse;
import com.khanh.fooddelivery.order_service.dto.request.CheckoutPreviewRequest;
import com.khanh.fooddelivery.order_service.dto.response.CheckoutPreviewResponse;
import com.khanh.fooddelivery.order_service.service.CheckoutPreviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders/checkout")
@RequiredArgsConstructor
public class CheckoutPreviewController {
    private final CheckoutPreviewService checkoutPreviewService;

    @PostMapping("/preview")
    public ApiResponse<CheckoutPreviewResponse> preview(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CheckoutPreviewRequest request) {
        return ApiResponse.success("Checkout preview calculated", checkoutPreviewService.preview(jwt, request));
    }
}
