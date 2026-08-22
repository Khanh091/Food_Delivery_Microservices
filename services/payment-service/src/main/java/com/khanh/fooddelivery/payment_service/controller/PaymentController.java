package com.khanh.fooddelivery.payment_service.controller;

import com.khanh.fooddelivery.payment_service.common.response.ApiResponse;
import com.khanh.fooddelivery.payment_service.dto.response.PaymentResponse;
import com.khanh.fooddelivery.payment_service.security.CurrentUserProvider;
import com.khanh.fooddelivery.payment_service.service.PaymentService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService payments;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/orders/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PaymentResponse> byOrder(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId) {
        return ApiResponse.success("Payment retrieved",
                payments.byOrder(currentUserProvider.getCurrentUserId(jwt), orderId));
    }

    @PostMapping("/orders/{orderId}/retry")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PaymentResponse> retry(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId) {
        return ApiResponse.success("Payment retry created",
                payments.retry(currentUserProvider.getCurrentUserId(jwt), orderId));
    }
}
