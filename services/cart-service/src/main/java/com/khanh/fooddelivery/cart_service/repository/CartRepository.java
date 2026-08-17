package com.khanh.fooddelivery.cart_service.repository;

import com.khanh.fooddelivery.cart_service.model.Cart;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface CartRepository {
    Optional<CartSnapshot> find(UUID ownerUserId, UUID branchId);

    List<CartSnapshot> findAll(UUID ownerUserId);

    boolean compareAndSet(UUID ownerUserId, UUID branchId, long expectedVersion, Cart cart);

    boolean compareAndDelete(UUID ownerUserId, UUID branchId, long expectedVersion);
}
