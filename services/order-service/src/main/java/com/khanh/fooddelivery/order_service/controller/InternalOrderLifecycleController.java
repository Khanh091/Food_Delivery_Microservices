package com.khanh.fooddelivery.order_service.controller;

import com.khanh.fooddelivery.order_service.common.response.ApiResponse;
import com.khanh.fooddelivery.order_service.service.OrderService;
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

    @PostMapping("/{id}/driver-assigned")
    public ApiResponse<Void> assigned(@PathVariable UUID id) {
        orders.deliveryAssigned(id);

        return ApiResponse.success(
                "Order preparing",
                null
        );
    }

    @PostMapping("/{id}/picked-up")
    public ApiResponse<Void> pickup(@PathVariable UUID id) {
        orders.pickedUp(id);

        return ApiResponse.success(
                "Order delivering",
                null
        );
    }

    @PostMapping("/{id}/delivered")
    public ApiResponse<Void> delivered(@PathVariable UUID id) {
        orders.delivered(id);

        return ApiResponse.success(
                "Order completed",
                null
        );
    }

    @PostMapping("/{id}/matching-failed")
    public ApiResponse<Void> failed(@PathVariable UUID id) {
        orders.matchingFailed(id);

        return ApiResponse.success(
                "Order cancelled",
                null
        );
    }
}