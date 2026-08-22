package com.khanh.fooddelivery.payment_service.client;

import com.khanh.fooddelivery.payment_service.common.response.ApiResponse;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "order-service")
public interface OrderServiceClient {
    @PostMapping("/internal/v1/orders/{id}/payment-paid")
    ApiResponse<Void> paymentPaid(@RequestHeader("X-Internal-Api-Key") String apiKey, @PathVariable UUID id);

    @PostMapping("/internal/v1/orders/{id}/payment-failed")
    ApiResponse<Void> paymentFailed(@RequestHeader("X-Internal-Api-Key") String apiKey, @PathVariable UUID id);
}
