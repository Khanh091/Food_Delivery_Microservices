package com.khanh.fooddelivery.payment_service.controller;

import com.khanh.fooddelivery.payment_service.common.response.ApiResponse;
import com.khanh.fooddelivery.payment_service.dto.request.PaymentWebhookRequest;
import com.khanh.fooddelivery.payment_service.dto.response.PaymentResponse;
import com.khanh.fooddelivery.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments/webhooks")
@RequiredArgsConstructor
public class PaymentWebhookController {
    private final PaymentService payments;

    @PostMapping("/mock")
    public ApiResponse<PaymentResponse> mock(@RequestBody PaymentWebhookRequest request) {
        return ApiResponse.success("Payment webhook processed", payments.webhook(request));
    }
}
