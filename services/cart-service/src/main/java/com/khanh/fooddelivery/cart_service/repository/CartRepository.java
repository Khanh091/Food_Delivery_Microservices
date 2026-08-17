package com.khanh.fooddelivery.cart_service.repository;

import com.khanh.fooddelivery.cart_service.model.Cart;
import java.util.Optional;
import java.util.UUID;

public interface CartRepository {
    Optional<CartSnapshot> find(UUID ownerUserId);

    boolean compareAndSet(UUID ownerUserId, long expectedVersion, Cart cart);

    boolean compareAndDelete(UUID ownerUserId, long expectedVersion);
}
