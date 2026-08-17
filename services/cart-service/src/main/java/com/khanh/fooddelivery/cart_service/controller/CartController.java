package com.khanh.fooddelivery.cart_service.controller;

import com.khanh.fooddelivery.cart_service.common.response.ApiResponse;
import com.khanh.fooddelivery.cart_service.dto.request.AddCartItemRequest;
import com.khanh.fooddelivery.cart_service.dto.request.ReplaceCartItemRequest;
import com.khanh.fooddelivery.cart_service.dto.request.UpdateCartItemQuantityRequest;
import com.khanh.fooddelivery.cart_service.dto.response.CartResponse;
import com.khanh.fooddelivery.cart_service.service.CartService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/carts")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @GetMapping
    public ApiResponse<CartResponse> get(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success("Cart retrieved", cartService.get(jwt));
    }

    @PostMapping("/items")
    public ApiResponse<CartResponse> add(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddCartItemRequest request) {
        return ApiResponse.success("Item added to cart", cartService.add(jwt, request));
    }

    @PostMapping("/items/replace")
    public ApiResponse<CartResponse> replace(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ReplaceCartItemRequest request) {
        return ApiResponse.success("Cart replaced", cartService.replace(jwt, request));
    }

    @PatchMapping("/items/{cartItemId}")
    public ApiResponse<CartResponse> updateQuantity(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID cartItemId,
            @Valid @RequestBody UpdateCartItemQuantityRequest request) {
        return ApiResponse.success("Cart item quantity updated", cartService.updateQuantity(jwt, cartItemId, request));
    }

    @DeleteMapping("/items/{cartItemId}")
    public ApiResponse<CartResponse> remove(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID cartItemId) {
        return ApiResponse.success("Cart item removed", cartService.remove(jwt, cartItemId));
    }

    @DeleteMapping
    public ApiResponse<CartResponse> clear(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success("Cart cleared", cartService.clear(jwt));
    }
}
