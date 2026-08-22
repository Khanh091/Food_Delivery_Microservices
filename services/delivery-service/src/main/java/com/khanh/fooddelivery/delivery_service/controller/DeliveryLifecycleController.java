package com.khanh.fooddelivery.delivery_service.controller;

import com.khanh.fooddelivery.delivery_service.common.response.ApiResponse;
import com.khanh.fooddelivery.delivery_service.dto.request.DeliveryMatchingRequest;
import com.khanh.fooddelivery.delivery_service.dto.response.*;
import com.khanh.fooddelivery.delivery_service.security.InternalRequestAuthenticator;
import com.khanh.fooddelivery.delivery_service.service.DeliveryLifecycleService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class DeliveryLifecycleController {
    private final DeliveryLifecycleService lifecycle;
    private final InternalRequestAuthenticator internalRequests;

    @PostMapping("/internal/v1/deliveries/matching")
    public ApiResponse<DeliveryResponse> matching(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String internalApiKey,
            @RequestBody DeliveryMatchingRequest request) {
        internalRequests.authenticate(internalApiKey);
        DeliveryResponse response = lifecycle.startMatching(request);
        String message = response.status().name().equals("MATCH_FAILED") ? "No driver available" : "Driver matching started";
        return ApiResponse.success(message, response);
    }

    @GetMapping("/api/v1/deliveries/driver/offers")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<List<DeliveryOfferResponse>> offers(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success("Delivery offers", lifecycle.offers(jwt));
    }

    @GetMapping("/api/v1/deliveries/me/offers/current")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<CurrentDeliveryOfferResponse> currentOffer(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponse.success("Current delivery offer", lifecycle.currentOffer(jwt).orElse(null));
    }

    @GetMapping("/api/v1/deliveries/me/active")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<DeliveryResponse> currentActiveDelivery(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponse.success("Current active delivery", lifecycle.currentActiveDelivery(jwt).orElse(null));
    }

    @PostMapping("/api/v1/deliveries/{id}/accept")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<DeliveryResponse> accept(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ApiResponse.success("Delivery assigned", lifecycle.accept(jwt, id));
    }

    @PostMapping("/api/v1/deliveries/{id}/reject")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<Void> reject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        lifecycle.reject(jwt, id);
        return ApiResponse.success("Delivery offer rejected", null);
    }

    @PostMapping("/api/v1/deliveries/{id}/picked-up")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<DeliveryResponse> pickup(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ApiResponse.success("Order picked up", lifecycle.pickup(jwt, id));
    }

    @PostMapping("/api/v1/deliveries/{id}/delivered")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<DeliveryResponse> delivered(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ApiResponse.success("Delivery completed", lifecycle.delivered(jwt, id));
    }

    @PostMapping("/api/v1/deliveries/{id}/restaurant-payment-confirmed")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<DeliveryResponse> restaurantPaymentConfirmed(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ApiResponse.success("Restaurant advance confirmed", lifecycle.confirmRestaurantPayment(jwt, id));
    }

    @PostMapping("/api/v1/deliveries/{id}/cash-collected")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<DeliveryResponse> cashCollected(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ApiResponse.success("Customer cash collected", lifecycle.collectCash(jwt, id));
    }
}
