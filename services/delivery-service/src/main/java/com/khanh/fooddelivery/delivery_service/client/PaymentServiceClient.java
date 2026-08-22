package com.khanh.fooddelivery.delivery_service.client;

import com.khanh.fooddelivery.delivery_service.common.response.ApiResponse;
import com.khanh.fooddelivery.delivery_service.client.dto.request.CashActionRequest;
import com.khanh.fooddelivery.delivery_service.client.dto.response.FinancialFactsResponse;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "payment-service")
public interface PaymentServiceClient {
    @GetMapping("/internal/v1/payments/orders/{orderId}/financial-facts")
    ApiResponse<FinancialFactsResponse> facts(@RequestHeader("X-Internal-Api-Key") String key,
                                              @PathVariable UUID orderId);

    @PostMapping("/internal/v1/payments/deliveries/{deliveryId}/restaurant-advance-confirmed")
    ApiResponse<Void> restaurantAdvance(@RequestHeader("X-Internal-Api-Key") String key,
                                         @PathVariable UUID deliveryId, @RequestBody CashActionRequest request);

    @PostMapping("/internal/v1/payments/deliveries/{deliveryId}/cash-collected")
    ApiResponse<Void> cashCollected(@RequestHeader("X-Internal-Api-Key") String key,
                                     @PathVariable UUID deliveryId, @RequestBody CashActionRequest request);

    @PostMapping("/internal/v1/payments/orders/{orderId}/delivery-completed")
    ApiResponse<Void> deliveryCompleted(@RequestHeader("X-Internal-Api-Key") String key,
                                         @PathVariable UUID orderId, @RequestBody CashActionRequest request);
}
