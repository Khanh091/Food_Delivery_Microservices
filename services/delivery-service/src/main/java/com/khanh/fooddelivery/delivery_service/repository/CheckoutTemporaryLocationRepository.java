package com.khanh.fooddelivery.delivery_service.repository;

import com.khanh.fooddelivery.delivery_service.model.CheckoutTemporaryLocation;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface CheckoutTemporaryLocationRepository {
    void save(CheckoutTemporaryLocation location, Duration ttl);
    Optional<CheckoutTemporaryLocation> findCurrent(UUID ownerUserId, UUID branchId);
}
