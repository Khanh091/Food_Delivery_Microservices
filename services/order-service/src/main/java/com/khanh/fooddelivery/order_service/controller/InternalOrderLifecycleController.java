package com.khanh.fooddelivery.order_service.controller;

import com.khanh.fooddelivery.order_service.common.response.ApiResponse;
import com.khanh.fooddelivery.order_service.service.OrderService;
import com.khanh.fooddelivery.order_service.security.InternalRequestAuthenticator;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/orders")
@RequiredArgsConstructor
public class InternalOrderLifecycleController {

    private final OrderService orders;
    private final InternalRequestAuthenticator internalRequests;

    @PostMapping("/{id}/driver-assigned")
    public ApiResponse<Void> assigned(
            @PathVariable UUID id,
            @org.springframework.web.bind.annotation.RequestHeader(
                    value = org.springframework.http.HttpHeaders.AUTHORIZATION,
                    required = false) String authorization) {
        internalRequests.authenticate(authorization);
        orders.deliveryAssigned(id);

        return ApiResponse.success(
                "Order preparing",
                null
        );
    }

    @PostMapping("/{id}/picked-up")
    public ApiResponse<Void> pickup(
            @PathVariable UUID id,
            @org.springframework.web.bind.annotation.RequestHeader(
                    value = org.springframework.http.HttpHeaders.AUTHORIZATION,
                    required = false) String authorization) {
        internalRequests.authenticate(authorization);
        orders.pickedUp(id);

        return ApiResponse.success(
                "Order delivering",
                null
        );
    }

    @PostMapping("/{id}/delivered")
    public ApiResponse<Void> delivered(
            @PathVariable UUID id,
            @org.springframework.web.bind.annotation.RequestHeader(
                    value = org.springframework.http.HttpHeaders.AUTHORIZATION,
                    required = false) String authorization) {
        internalRequests.authenticate(authorization);
        orders.delivered(id);

        return ApiResponse.success(
                "Order completed",
                null
        );
    }

    @PostMapping("/{id}/matching-failed")
    public ApiResponse<Void> failed(
            @PathVariable UUID id,
            @org.springframework.web.bind.annotation.RequestHeader(
                    value = org.springframework.http.HttpHeaders.AUTHORIZATION,
                    required = false) String authorization) {
        internalRequests.authenticate(authorization);
        orders.matchingFailed(id);

        return ApiResponse.success(
                "Order cancelled",
                null
        );
    }

}
