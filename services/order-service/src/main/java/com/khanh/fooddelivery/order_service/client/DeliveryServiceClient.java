package com.khanh.fooddelivery.order_service.client;

import com.khanh.fooddelivery.order_service.client.dto.request.DeliveryMatchingRequest;
import com.khanh.fooddelivery.order_service.client.dto.request.DeliveryQuoteRequest;
import com.khanh.fooddelivery.order_service.client.dto.response.CheckoutTemporaryLocationResponse;
import com.khanh.fooddelivery.order_service.client.dto.response.DeliveryQuoteResponse;
import com.khanh.fooddelivery.order_service.common.response.ApiResponse;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "delivery-service")
public interface DeliveryServiceClient {

    @PostMapping("/internal/v1/delivery/quotes")
    ApiResponse<DeliveryQuoteResponse> createQuote(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestBody DeliveryQuoteRequest request
    );

    @PostMapping("/internal/v1/deliveries/matching")
    ApiResponse<Object> startMatching(
            @RequestHeader("X-Internal-Api-Key") String internalApiKey,
            @RequestBody DeliveryMatchingRequest request
    );

    @GetMapping("/internal/v1/delivery/checkout-locations/branches/{branchId}/current")
    ApiResponse<CheckoutTemporaryLocationResponse> getCurrentCheckoutLocation(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable UUID branchId
    );

}
