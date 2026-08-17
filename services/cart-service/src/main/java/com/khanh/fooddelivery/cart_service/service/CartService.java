package com.khanh.fooddelivery.cart_service.service;

import com.khanh.fooddelivery.cart_service.dto.request.AddCartItemRequest;
import com.khanh.fooddelivery.cart_service.dto.request.ReplaceCartItemRequest;
import com.khanh.fooddelivery.cart_service.dto.request.UpdateCartItemQuantityRequest;
import com.khanh.fooddelivery.cart_service.dto.response.CartResponse;
import com.khanh.fooddelivery.cart_service.dto.response.internal.InternalCartSnapshotResponse;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

public interface CartService {
    CartResponse get(Jwt jwt);

    InternalCartSnapshotResponse getInternalSnapshot(Jwt jwt);

    CartResponse add(Jwt jwt, AddCartItemRequest request);

    CartResponse replace(Jwt jwt, ReplaceCartItemRequest request);

    CartResponse updateQuantity(Jwt jwt, UUID cartItemId, UpdateCartItemQuantityRequest request);

    CartResponse remove(Jwt jwt, UUID cartItemId);

    CartResponse clear(Jwt jwt);
}
