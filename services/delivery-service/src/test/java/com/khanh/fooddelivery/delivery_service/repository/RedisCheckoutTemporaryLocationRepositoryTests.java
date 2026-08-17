package com.khanh.fooddelivery.delivery_service.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisCheckoutTemporaryLocationRepositoryTests {
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @Test
    void malformedEphemeralValueIsDiscardedAndBehavesLikeNoCurrentLocation() {
        UUID ownerUserId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        String key = "food-delivery:delivery:checkout-location:v1:" + ownerUserId + ':' + branchId;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("{not-valid-json");

        var repository = new RedisCheckoutTemporaryLocationRepository(redisTemplate, new ObjectMapper());

        assertThat(repository.findCurrent(ownerUserId, branchId)).isEmpty();
        verify(redisTemplate).delete(key);
    }
}
