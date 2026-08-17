package com.khanh.fooddelivery.cart_service.repository;

import com.khanh.fooddelivery.cart_service.model.Cart;
import java.time.Instant;

public record CartSnapshot(Cart cart, Instant expiresAt) {}
