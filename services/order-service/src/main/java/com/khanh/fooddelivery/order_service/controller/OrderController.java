package com.khanh.fooddelivery.order_service.controller;

import com.khanh.fooddelivery.order_service.common.response.ApiResponse;
import com.khanh.fooddelivery.order_service.dto.request.CreateOrderRequest;
import com.khanh.fooddelivery.order_service.dto.request.RejectOrderRequest;
import com.khanh.fooddelivery.order_service.dto.response.OrderResponse;
import com.khanh.fooddelivery.order_service.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orders;

    @PostMapping
    public ApiResponse<OrderResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        return ApiResponse.success(
                "Order created",
                orders.create(jwt, request)
        );
    }

    @GetMapping("/me")
    public ApiResponse<List<OrderResponse>> mine(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponse.success(
                "Orders retrieved",
                orders.mine(jwt)
        );
    }

    @GetMapping("/restaurants/{restaurantId}")
    public ApiResponse<List<OrderResponse>> restaurant(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID restaurantId
    ) {
        return ApiResponse.success(
                "Restaurant orders retrieved",
                orders.restaurant(jwt, restaurantId)
        );
    }

    @PostMapping("/{orderId}/accept")
    public ApiResponse<OrderResponse> accept(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID orderId
    ) {
        return ApiResponse.success(
                "Order accepted",
                orders.accept(jwt, orderId)
        );
    }

    @PostMapping("/{orderId}/reject")
    public ApiResponse<OrderResponse> reject(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID orderId,
            @Valid @RequestBody RejectOrderRequest request
    ) {
        return ApiResponse.success(
                "Order rejected",
                orders.reject(jwt, orderId, request)
        );
    }
}