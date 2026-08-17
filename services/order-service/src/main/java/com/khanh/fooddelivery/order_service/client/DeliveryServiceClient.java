package com.khanh.fooddelivery.order_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "delivery-service")
public interface DeliveryServiceClient {
    @PostMapping("/internal/v1/delivery/quotes")
    RemoteApiResponse<DeliveryQuoteResponse> createQuote(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization, @RequestBody DeliveryQuoteRequest request);

    @GetMapping("/internal/v1/delivery/checkout-locations/branches/{branchId}/current")
    RemoteApiResponse<CheckoutTemporaryLocationResponse> getCurrentCheckoutLocation(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization, @PathVariable UUID branchId);

    record DeliveryQuoteRequest(UUID branchId, DeliveryTargetRequest target) {
        public DeliveryQuoteRequest(UUID branchId, UUID addressId) { this(branchId, DeliveryTargetRequest.savedAddress(addressId)); }
    }
    record DeliveryTargetRequest(String type, UUID addressId, UUID temporaryLocationId) {
        static DeliveryTargetRequest savedAddress(UUID addressId) { return new DeliveryTargetRequest("SAVED_ADDRESS", addressId, null); }
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    record CheckoutTemporaryLocationResponse(UUID id, UUID branchId, String formattedAddress, String addressLine,
                                             String ward, String district, String city, BigDecimal latitude,
                                             BigDecimal longitude, Instant expiresAt) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DeliveryQuoteResponse(
            UUID quoteId, boolean serviceable, String currency, BigDecimal deliveryFee, long distanceMeters,
            long estimatedDurationMinutes, String pricingPolicyVersion, Instant calculatedAt, Instant expiresAt) {}
}
