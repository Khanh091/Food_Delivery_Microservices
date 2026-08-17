package com.khanh.fooddelivery.cart_service.controller;

import com.khanh.fooddelivery.cart_service.common.response.ApiResponse;
import com.khanh.fooddelivery.cart_service.dto.request.AddCartItemRequest;
import com.khanh.fooddelivery.cart_service.dto.request.UpdateCartItemConfigurationRequest;
import com.khanh.fooddelivery.cart_service.dto.request.UpdateCartItemQuantityRequest;
import com.khanh.fooddelivery.cart_service.dto.response.CartResponse;
import com.khanh.fooddelivery.cart_service.dto.response.CartSummaryResponse;
import com.khanh.fooddelivery.cart_service.service.CartService;
import jakarta.validation.Valid;
import java.util.UUID;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/carts")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @GetMapping
    public ApiResponse<List<CartSummaryResponse>> list(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success("Carts retrieved", cartService.list(jwt));
    }

    @GetMapping("/branches/{branchId}")
    public ApiResponse<CartResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID branchId) {
        return ApiResponse.success("Cart retrieved", cartService.get(jwt, branchId));
    }

    @PostMapping("/branches/{branchId}/items")
    public ApiResponse<CartResponse> add(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID branchId,
            @Valid @RequestBody AddCartItemRequest request) {
        return ApiResponse.success("Item added to cart", cartService.add(jwt, branchId, request));
    }

    @PatchMapping("/branches/{branchId}/items/{cartItemId}")
    public ApiResponse<CartResponse> updateQuantity(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID branchId,
            @PathVariable UUID cartItemId,
            @Valid @RequestBody UpdateCartItemQuantityRequest request) {
        return ApiResponse.success("Cart item quantity updated", cartService.updateQuantity(jwt, branchId, cartItemId, request));
    }

    @PutMapping("/branches/{branchId}/items/{cartItemId}/configuration")
    public ApiResponse<CartResponse> updateConfiguration(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID branchId,
            @PathVariable UUID cartItemId,
            @Valid @RequestBody UpdateCartItemConfigurationRequest request) {
        return ApiResponse.success(
                "Cart item configuration updated", cartService.updateConfiguration(jwt, branchId, cartItemId, request));
    }

    @DeleteMapping("/branches/{branchId}/items/{cartItemId}")
    public ApiResponse<CartResponse> remove(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID branchId, @PathVariable UUID cartItemId) {
        return ApiResponse.success("Cart item removed", cartService.remove(jwt, branchId, cartItemId));
    }

    @DeleteMapping("/branches/{branchId}")
    public ApiResponse<CartResponse> clear(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID branchId) {
        return ApiResponse.success("Cart cleared", cartService.clear(jwt, branchId));
    }
}
