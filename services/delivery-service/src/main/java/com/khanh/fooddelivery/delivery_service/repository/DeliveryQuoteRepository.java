package com.khanh.fooddelivery.delivery_service.repository;

import com.khanh.fooddelivery.delivery_service.model.DeliveryQuote;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryQuoteRepository {
    void save(DeliveryQuote quote, Duration ttl);
    Optional<DeliveryQuote> findById(UUID quoteId);
}
