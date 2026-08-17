package com.khanh.fooddelivery.delivery_service.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.khanh.fooddelivery.delivery_service.model.DeliveryQuote;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisDeliveryQuoteRepository implements DeliveryQuoteRepository {
    private static final String KEY_PREFIX = "food-delivery:delivery:quote:v1:";
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void save(DeliveryQuote quote, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key(quote.quoteId()), objectMapper.writeValueAsString(quote), ttl);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize delivery quote", exception);
        }
    }

    @Override
    public Optional<DeliveryQuote> findById(UUID quoteId) {
        String value = redisTemplate.opsForValue().get(key(quoteId));
        if (value == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(value, DeliveryQuote.class));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to deserialize delivery quote", exception);
        }
    }

    private String key(UUID quoteId) { return KEY_PREFIX + quoteId; }
}
