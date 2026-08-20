package com.khanh.fooddelivery.order_service.client;

import com.khanh.fooddelivery.order_service.client.dto.request.CheckoutItemRequest;
import com.khanh.fooddelivery.order_service.client.dto.request.CheckoutItemsValidationRequest;
import com.khanh.fooddelivery.order_service.client.dto.response.CheckoutItemsValidationResponse;
import com.khanh.fooddelivery.order_service.client.dto.response.ValidatedCheckoutItemResponse;
import com.khanh.fooddelivery.order_service.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "catalog-service")
public interface CatalogServiceClient {

    @PostMapping("/internal/v1/catalog/checkout-items/validate")
    ApiResponse<CheckoutItemsValidationResponse> validateCheckoutItems(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestBody CheckoutItemsValidationRequest request
    );

}
