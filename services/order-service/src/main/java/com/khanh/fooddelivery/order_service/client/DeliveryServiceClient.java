package com.khanh.fooddelivery.order_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "delivery-service")
public interface DeliveryServiceClient {
    @PostMapping("/internal/v1/delivery/quotes")
    RemoteApiResponse<DeliveryQuoteResponse> createQuote(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization, @RequestBody DeliveryQuoteRequest request);

    record DeliveryQuoteRequest(UUID branchId, UUID addressId) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DeliveryQuoteResponse(
            UUID quoteId, boolean serviceable, String currency, BigDecimal deliveryFee, long distanceMeters,
            long estimatedDurationMinutes, String pricingPolicyVersion, Instant calculatedAt, Instant expiresAt) {}
}
