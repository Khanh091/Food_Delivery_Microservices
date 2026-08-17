package com.khanh.fooddelivery.delivery_service.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.khanh.fooddelivery.delivery_service.model.CheckoutTemporaryLocation;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisCheckoutTemporaryLocationRepository implements CheckoutTemporaryLocationRepository {
    private static final String KEY_PREFIX = "food-delivery:delivery:checkout-location:v1:";
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void save(CheckoutTemporaryLocation location, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key(location.ownerUserId(), location.branchId()), objectMapper.writeValueAsString(location), ttl);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize checkout temporary location", exception);
        }
    }

    @Override
    public Optional<CheckoutTemporaryLocation> findCurrent(UUID ownerUserId, UUID branchId) {
        String value = redisTemplate.opsForValue().get(key(ownerUserId, branchId));
        if (value == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(value, CheckoutTemporaryLocation.class));
        } catch (JsonProcessingException exception) {
            // Checkout locations are isolated, short-lived state. A value written by an
            // earlier development shape must never turn a normal Checkout load into 500.
            redisTemplate.delete(key(ownerUserId, branchId));
            return Optional.empty();
        }
    }

    private String key(UUID userId, UUID branchId) { return KEY_PREFIX + userId + ':' + branchId; }
}
