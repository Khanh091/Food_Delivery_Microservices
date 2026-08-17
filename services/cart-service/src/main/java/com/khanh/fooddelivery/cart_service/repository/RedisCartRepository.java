package com.khanh.fooddelivery.cart_service.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.khanh.fooddelivery.cart_service.config.CartProperties;
import com.khanh.fooddelivery.cart_service.exception.AppException;
import com.khanh.fooddelivery.cart_service.exception.ErrorCode;
import com.khanh.fooddelivery.cart_service.model.Cart;
import java.time.Instant;
import java.util.ArrayList;
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
    private static final String CART_KEY_PREFIX = "food-delivery:cart:v2:user:";
    private static final String INDEX_KEY_PREFIX = "food-delivery:cart:v2:user:";
    private static final DefaultRedisScript<Long> COMPARE_AND_SET = new DefaultRedisScript<>(
            "local current = redis.call('GET', KEYS[1]) "
                    + "if tonumber(ARGV[1]) == 0 then "
                    + "  if current then return 0 end "
                    + "else "
                    + "  if not current then return 0 end "
                    + "  local stored = cjson.decode(current) "
                    + "  if tonumber(stored.version) ~= tonumber(ARGV[1]) then return 0 end "
                    + "end "
                    + "redis.call('SET', KEYS[1], ARGV[2], 'EX', ARGV[3]) "
                    + "redis.call('ZADD', KEYS[2], ARGV[5], ARGV[4]) "
                    + "local indexTtl = redis.call('TTL', KEYS[2]) "
                    + "if indexTtl < tonumber(ARGV[3]) then redis.call('EXPIRE', KEYS[2], ARGV[3]) end "
                    + "return 1",
            Long.class);
    private static final DefaultRedisScript<Long> COMPARE_AND_DELETE = new DefaultRedisScript<>(
            "local current = redis.call('GET', KEYS[1]) "
                    + "if not current then return 0 end "
                    + "local stored = cjson.decode(current) "
                    + "if tonumber(stored.version) ~= tonumber(ARGV[1]) then return 0 end "
                    + "redis.call('DEL', KEYS[1]) "
                    + "redis.call('ZREM', KEYS[2], ARGV[2]) "
                    + "if redis.call('ZCARD', KEYS[2]) == 0 then redis.call('DEL', KEYS[2]) end "
                    + "return 1",
            Long.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final CartProperties properties;

    @Override
    public Optional<CartSnapshot> find(UUID ownerUserId, UUID branchId) {
        String key = cartKey(ownerUserId, branchId);
        String json = redis.opsForValue().get(key);
        return json == null ? Optional.empty() : Optional.of(snapshot(key, json));
    }

    @Override
    public List<CartSnapshot> findAll(UUID ownerUserId) {
        String indexKey = indexKey(ownerUserId);
        long now = Instant.now().toEpochMilli();
        redis.opsForZSet().removeRangeByScore(indexKey, Double.NEGATIVE_INFINITY, now);
        List<String> branchIds = redis.opsForZSet().range(indexKey, 0, -1).stream().toList();
        if (branchIds.isEmpty()) return List.of();

        List<String> keys = branchIds.stream()
                .map(branchId -> cartKey(ownerUserId, UUID.fromString(branchId)))
                .toList();
        List<String> values = redis.opsForValue().multiGet(keys);
        List<CartSnapshot> snapshots = new ArrayList<>();
        for (int index = 0; index < branchIds.size(); index++) {
            String json = values == null ? null : values.get(index);
            if (json == null) {
                redis.opsForZSet().remove(indexKey, branchIds.get(index));
            } else {
                snapshots.add(snapshot(keys.get(index), json));
            }
        }
        Long remainingMembers = redis.opsForZSet().size(indexKey);
        if (remainingMembers != null && remainingMembers == 0) redis.delete(indexKey);
        return snapshots;
    }

    @Override
    public boolean compareAndSet(UUID ownerUserId, UUID branchId, long expectedVersion, Cart cart) {
        long ttlSeconds = properties.ttl().toSeconds();
        Long result = redis.execute(
                COMPARE_AND_SET,
                List.of(cartKey(ownerUserId, branchId), indexKey(ownerUserId)),
                Long.toString(expectedVersion),
                write(cart),
                Long.toString(ttlSeconds),
                branchId.toString(),
                Long.toString(Instant.now().plusSeconds(ttlSeconds).toEpochMilli()));
        return Long.valueOf(1L).equals(result);
    }

    @Override
    public boolean compareAndDelete(UUID ownerUserId, UUID branchId, long expectedVersion) {
        Long result = redis.execute(
                COMPARE_AND_DELETE,
                List.of(cartKey(ownerUserId, branchId), indexKey(ownerUserId)),
                Long.toString(expectedVersion),
                branchId.toString());
        return Long.valueOf(1L).equals(result);
    }

    private CartSnapshot snapshot(String key, String json) {
        try {
            Cart cart = objectMapper.readValue(json, Cart.class);
            Long seconds = redis.getExpire(key);
            Instant expiresAt = seconds != null && seconds > 0 ? Instant.now().plusSeconds(seconds) : null;
            return new CartSnapshot(cart, expiresAt);
        } catch (JsonProcessingException exception) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Unable to read cart data");
        }
    }

    private String write(Cart cart) {
        try {
            return objectMapper.writeValueAsString(cart);
        } catch (JsonProcessingException exception) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Unable to write cart data");
        }
    }

    private String cartKey(UUID ownerUserId, UUID branchId) {
        return CART_KEY_PREFIX + ownerUserId + ":branch:" + branchId;
    }

    private String indexKey(UUID ownerUserId) {
        return INDEX_KEY_PREFIX + ownerUserId + ":branches";
    }
}
