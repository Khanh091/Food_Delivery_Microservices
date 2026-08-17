package com.khanh.fooddelivery.cart_service.service;

import com.khanh.fooddelivery.cart_service.dto.request.AddCartItemRequest;
import com.khanh.fooddelivery.cart_service.dto.request.UpdateCartItemConfigurationRequest;
import com.khanh.fooddelivery.cart_service.dto.request.UpdateCartItemQuantityRequest;
import com.khanh.fooddelivery.cart_service.dto.response.CartResponse;
import com.khanh.fooddelivery.cart_service.dto.response.CartSummaryResponse;
import java.util.List;
import com.khanh.fooddelivery.cart_service.dto.response.internal.InternalCartSnapshotResponse;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

public interface CartService {
    List<CartSummaryResponse> list(Jwt jwt);

    CartResponse get(Jwt jwt, UUID branchId);

    InternalCartSnapshotResponse getInternalSnapshot(Jwt jwt, UUID branchId);

    CartResponse add(Jwt jwt, UUID branchId, AddCartItemRequest request);

    CartResponse updateQuantity(Jwt jwt, UUID branchId, UUID cartItemId, UpdateCartItemQuantityRequest request);

    CartResponse updateConfiguration(
            Jwt jwt, UUID branchId, UUID cartItemId, UpdateCartItemConfigurationRequest request);

    CartResponse remove(Jwt jwt, UUID branchId, UUID cartItemId);

    CartResponse clear(Jwt jwt, UUID branchId);
}
