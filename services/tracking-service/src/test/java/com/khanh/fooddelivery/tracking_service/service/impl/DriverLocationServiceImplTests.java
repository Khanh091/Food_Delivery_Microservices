package com.khanh.fooddelivery.tracking_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.tracking_service.client.DriverServiceClient;
import com.khanh.fooddelivery.tracking_service.client.dto.response.DriverProfileResponse;
import com.khanh.fooddelivery.tracking_service.dto.request.DriverLocationUpdateRequest;
import com.khanh.fooddelivery.tracking_service.dto.response.NearestDriverResponse;
import com.khanh.fooddelivery.tracking_service.exception.AppException;
import com.khanh.fooddelivery.tracking_service.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoSearchCommandArgs;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DriverLocationServiceImplTests {

    private static final UUID DRIVER_A = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private static final UUID DRIVER_B = UUID.fromString("00000000-0000-0000-0000-000000000302");
    private static final UUID DRIVER_C = UUID.fromString("00000000-0000-0000-0000-000000000303");
    private static final UUID DRIVER_D = UUID.fromString("00000000-0000-0000-0000-000000000304");
    private static final UUID KEYCLOAK_SUB = UUID.fromString("00000000-0000-0000-0000-000000000305");
    private static final UUID CANONICAL_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000306");

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private DriverServiceClient drivers;
    @Mock
    private GeoOperations<String, String> geo;
    @Mock
    private HashOperations<String, Object, Object> metadata;

    private DriverLocationServiceImpl locations;

    @BeforeEach
    void setUp() {
        locations = new DriverLocationServiceImpl(redis, drivers);
        ReflectionTestUtils.setField(locations, "staleAfter", Duration.ofSeconds(45));
        ReflectionTestUtils.setField(locations, "maxAccuracy", 100d);
        lenient().when(redis.opsForGeo()).thenReturn(geo);
        lenient().when(redis.opsForHash()).thenReturn(metadata);
    }

    @Test
    void update_saves_driver_point_and_location_metadata() {
        Instant recordedAt = Instant.parse("2026-08-20T12:00:00Z");
        when(drivers.profile("Bearer token")).thenReturn(activeProfile(CANONICAL_USER_ID));

        var response = locations.update(
                "Bearer token",
                new DriverLocationUpdateRequest(
                        new BigDecimal("10.1234"),
                        new BigDecimal("20.5678"),
                        8d,
                        recordedAt
                )
        );

        assertThat(response.driverId()).isEqualTo(CANONICAL_USER_ID);
        assertThat(response.driverId()).isNotEqualTo(KEYCLOAK_SUB);
        assertThat(response.latitude()).isEqualByComparingTo("10.1234");
        assertThat(response.longitude()).isEqualByComparingTo("20.5678");
        assertThat(response.recordedAt()).isEqualTo(recordedAt);
        verify(geo).add(
                eq("tracking:drivers:geo"),
                eq(new Point(20.5678, 10.1234)),
                eq(CANONICAL_USER_ID.toString())
        );
        verify(metadata).putAll(
                eq("tracking:driver:location:" + CANONICAL_USER_ID),
                any(Map.class)
        );
        verify(redis).expire(eq("tracking:driver:location:" + CANONICAL_USER_ID), eq(Duration.ofSeconds(135)));
    }

    @Test
    void heartbeat_with_same_measurement_refreshes_server_freshness() {
        Instant measuredAt = Instant.now().minusSeconds(60);
        when(drivers.profile("Bearer token")).thenReturn(activeProfile(CANONICAL_USER_ID));
        DriverLocationUpdateRequest heartbeat = new DriverLocationUpdateRequest(
                new BigDecimal("10.1234"),
                new BigDecimal("20.5678"),
                8d,
                measuredAt
        );

        var first = locations.update("Bearer token", heartbeat);
        var second = locations.update("Bearer token", heartbeat);

        assertThat(first.recordedAt()).isEqualTo(measuredAt);
        assertThat(second.recordedAt()).isEqualTo(measuredAt);
        assertThat(second.updatedAt()).isAfterOrEqualTo(first.updatedAt());
        verify(metadata, times(2)).putAll(
                eq("tracking:driver:location:" + CANONICAL_USER_ID),
                any(Map.class)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDING", "SUSPENDED", "REJECTED"})
    void non_active_driver_cannot_update_location(String status) {
        when(drivers.profile("Bearer token"))
                .thenReturn(new DriverProfileResponse(UUID.randomUUID(), 1L, CANONICAL_USER_ID, status));

        assertThatThrownBy(() -> locations.update(
                "Bearer token",
                new DriverLocationUpdateRequest(
                        new BigDecimal("10"),
                        new BigDecimal("20"),
                        10d,
                        null
                )
        )).isInstanceOf(AppException.class)
                .extracting(exception -> ((AppException) exception).getErrorCode())
                .isEqualTo(ErrorCode.DRIVER_NOT_ACTIVE);

        verify(redis, never()).opsForGeo();
    }

    @Test
    void location_accuracy_above_configured_limit_is_rejected() {
        when(drivers.profile("Bearer token")).thenReturn(activeProfile(CANONICAL_USER_ID));

        assertThatThrownBy(() -> locations.update(
                "Bearer token",
                new DriverLocationUpdateRequest(
                        new BigDecimal("10"),
                        new BigDecimal("20"),
                        101d,
                        null
                )
        )).isInstanceOf(AppException.class)
                .extracting(exception -> ((AppException) exception).getErrorCode())
                .isEqualTo(ErrorCode.LOCATION_ACCURACY_TOO_LOW);

        verify(geo, never()).add(anyString(), any(Point.class), anyString());
    }

    @Test
    void nearest_returns_only_fresh_accurate_locations_in_distance_order() {
        Instant now = Instant.now();
        when(geo.search(
                eq("tracking:drivers:geo"),
                any(),
                any(Distance.class),
                any(GeoSearchCommandArgs.class)
        )).thenReturn(new GeoResults<>(List.of(
                result(DRIVER_A, 500),
                result(DRIVER_B, 900),
                result(DRIVER_C, 2000),
                result(DRIVER_D, 600)
        )));
        when(metadata.entries(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            UUID driverId = UUID.fromString(key.substring(key.lastIndexOf(':') + 1));
            Map<Object, Object> values = new HashMap<>();
            values.put("updatedAt", driverId.equals(DRIVER_A)
                    ? now.minusSeconds(120).toString()
                    : now.toString());
            values.put("accuracy", driverId.equals(DRIVER_D) ? "150" : "10");
            return values;
        });

        List<NearestDriverResponse> result = locations.nearest(
                new BigDecimal("10.0000"),
                new BigDecimal("20.0000"),
                2000,
                20
        );

        assertThat(result).extracting(NearestDriverResponse::driverId)
                .containsExactly(DRIVER_B, DRIVER_C);
        assertThat(result).extracting(NearestDriverResponse::distanceMeters)
                .containsExactly(900L, 2000L);

        ArgumentCaptor<Distance> radius = ArgumentCaptor.forClass(Distance.class);
        ArgumentCaptor<GeoSearchCommandArgs> searchArgs =
                ArgumentCaptor.forClass(GeoSearchCommandArgs.class);
        verify(geo).search(
                eq("tracking:drivers:geo"),
                any(),
                radius.capture(),
                searchArgs.capture()
        );
        assertThat(radius.getValue().getMetric()).isEqualTo(Metrics.KILOMETERS);
        assertThat(radius.getValue().getValue()).isEqualTo(2d);
        assertThat(searchArgs.getValue().getSortDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(searchArgs.getValue().getLimit()).isEqualTo(20L);
    }

    @Test
    void redis_failure_is_not_converted_to_a_random_driver() {
        when(geo.search(
                eq("tracking:drivers:geo"),
                any(),
                any(Distance.class),
                any(GeoSearchCommandArgs.class)
        )).thenThrow(new IllegalStateException("Redis unavailable"));

        assertThatThrownBy(() -> locations.nearest(
                new BigDecimal("10"),
                new BigDecimal("20"),
                2000,
                20
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("Redis unavailable");
    }

    private GeoResult<GeoLocation<String>> result(UUID driverId, long distanceMeters) {
        GeoLocation<String> location = new RedisGeoCommands.GeoLocation<>(
                driverId.toString(),
                new Point(20, 10)
        );
        return new GeoResult<>(
                location,
                new Distance(distanceMeters / 1000d, Metrics.KILOMETERS)
        );
    }

    private DriverProfileResponse activeProfile(UUID userId) {
        return new DriverProfileResponse(UUID.randomUUID(), 1L, userId, "ACTIVE");
    }
}
