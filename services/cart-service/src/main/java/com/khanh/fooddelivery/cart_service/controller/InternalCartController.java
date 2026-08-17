package com.khanh.fooddelivery.cart_service.controller;

import com.khanh.fooddelivery.cart_service.common.response.ApiResponse;
import com.khanh.fooddelivery.cart_service.dto.response.internal.InternalCartSnapshotResponse;
import com.khanh.fooddelivery.cart_service.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/carts")
@RequiredArgsConstructor
public class InternalCartController {
    private final CartService cartService;

    @GetMapping("/me")
    public ApiResponse<InternalCartSnapshotResponse> getCurrentSnapshot(
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success("Cart snapshot retrieved", cartService.getInternalSnapshot(jwt));
    }
}
