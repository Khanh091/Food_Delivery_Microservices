package com.khanh.fooddelivery.order_service.client;

import com.khanh.fooddelivery.order_service.client.dto.request.InternalCreatePaymentRequest;
import com.khanh.fooddelivery.order_service.client.dto.response.PaymentResponse;
import com.khanh.fooddelivery.order_service.common.response.ApiResponse;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "payment-service")
public interface PaymentServiceClient {
    @PostMapping("/internal/v1/payments")
    ApiResponse<PaymentResponse> create(@RequestHeader("X-Internal-Api-Key") String key,
                                        @RequestBody InternalCreatePaymentRequest request);

    @PostMapping("/internal/v1/payments/orders/{orderId}/refund")
    ApiResponse<PaymentResponse> refund(@RequestHeader("X-Internal-Api-Key") String key,
                                         @PathVariable UUID orderId);

    @PostMapping("/internal/v1/payments/orders/{orderId}/cancel")
    ApiResponse<PaymentResponse> cancel(@RequestHeader("X-Internal-Api-Key") String key,
                                        @PathVariable UUID orderId);

    @PostMapping("/internal/v1/payments/orders/{orderId}/collected")
    ApiResponse<PaymentResponse> collected(@RequestHeader("X-Internal-Api-Key") String key,
                                           @PathVariable UUID orderId);
}
