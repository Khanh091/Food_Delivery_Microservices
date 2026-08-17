package com.khanh.fooddelivery.cart_service.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.khanh.fooddelivery.cart_service.config.CartProperties;
import com.khanh.fooddelivery.cart_service.exception.AppException;
import com.khanh.fooddelivery.cart_service.exception.ErrorCode;
import com.khanh.fooddelivery.cart_service.model.Cart;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisCartRepository implements CartRepository {
    private static final String KEY_PREFIX = "food-delivery:cart:v1:user:";
    private static final DefaultRedisScript<Long> COMPARE_AND_SET =
            new DefaultRedisScript<>(
                    "local current = redis.call('GET', KEYS[1]) "
                            + "if tonumber(ARGV[1]) == 0 then "
                            + "  if current then return 0 end "
                            + "else "
                            + "  if not current then return 0 end "
                            + "  local stored = cjson.decode(current) "
                            + "  if tonumber(stored.version) ~= tonumber(ARGV[1]) then return 0 end "
                            + "end "
                            + "redis.call('SET', KEYS[1], ARGV[2], 'EX', ARGV[3]) "
                            + "return 1",
                    Long.class);
    private static final DefaultRedisScript<Long> COMPARE_AND_DELETE =
            new DefaultRedisScript<>(
                    "local current = redis.call('GET', KEYS[1]) "
                            + "if not current then return 0 end "
                            + "local stored = cjson.decode(current) "
                            + "if tonumber(stored.version) ~= tonumber(ARGV[1]) then return 0 end "
                            + "redis.call('DEL', KEYS[1]) "
                            + "return 1",
                    Long.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final CartProperties properties;

    @Override
    public Optional<CartSnapshot> find(UUID ownerUserId) {
        String key = key(ownerUserId);
        String json = redis.opsForValue().get(key);
        if (json == null) return Optional.empty();
        try {
            Cart cart = objectMapper.readValue(json, Cart.class);
            Long seconds = redis.getExpire(key);
            Instant expiresAt = seconds != null && seconds > 0 ? Instant.now().plusSeconds(seconds) : null;
            return Optional.of(new CartSnapshot(cart, expiresAt));
        } catch (JsonProcessingException exception) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Unable to read cart data");
        }
    }

    @Override
    public boolean compareAndSet(UUID ownerUserId, long expectedVersion, Cart cart) {
        Long result =
                redis.execute(
                        COMPARE_AND_SET,
                        List.of(key(ownerUserId)),
                        Long.toString(expectedVersion),
                        write(cart),
                        Long.toString(properties.ttl().toSeconds()));
        return Long.valueOf(1L).equals(result);
    }

    @Override
    public boolean compareAndDelete(UUID ownerUserId, long expectedVersion) {
        Long result =
                redis.execute(
                        COMPARE_AND_DELETE,
                        List.of(key(ownerUserId)),
                        Long.toString(expectedVersion));
        return Long.valueOf(1L).equals(result);
    }

    private String write(Cart cart) {
        try {
            return objectMapper.writeValueAsString(cart);
        } catch (JsonProcessingException exception) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Unable to write cart data");
        }
    }

    private String key(UUID ownerUserId) {
        return KEY_PREFIX + ownerUserId;
    }
}
