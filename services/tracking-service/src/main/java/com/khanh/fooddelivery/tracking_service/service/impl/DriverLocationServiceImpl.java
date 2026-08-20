package com.khanh.fooddelivery.tracking_service.service.impl;

import com.khanh.fooddelivery.tracking_service.client.DriverServiceClient;
import com.khanh.fooddelivery.tracking_service.dto.request.DriverLocationUpdateRequest;
import com.khanh.fooddelivery.tracking_service.dto.response.DriverLocationResponse;
import com.khanh.fooddelivery.tracking_service.dto.response.NearestDriverResponse;
import com.khanh.fooddelivery.tracking_service.service.DriverLocationService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoSearchCommandArgs;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DriverLocationServiceImpl implements DriverLocationService {

    private static final String GEO_KEY = "tracking:drivers:geo";

    private final StringRedisTemplate redis;
    private final DriverServiceClient drivers;

    @Value("${tracking.driver-location.stale-after:45s}")
    private Duration staleAfter;

    @Value("${tracking.driver-location.max-accuracy-meters:100}")
    private double maxAccuracy;

    @Override
    public DriverLocationResponse update(
            UUID driverId,
            String authorization,
            DriverLocationUpdateRequest request
    ) {
        if (!drivers.active(authorization, driverId)) {
            throw new IllegalStateException("Driver is not active");
        }
        if (request.accuracyMeters() > maxAccuracy) {
            throw new IllegalArgumentException("Location accuracy is too low");
        }

        Instant recordedAt = request.recordedAt() == null
                ? Instant.now()
                : request.recordedAt();
        Instant updatedAt = Instant.now();
        redis.opsForGeo().add(
                GEO_KEY,
                new Point(request.longitude().doubleValue(), request.latitude().doubleValue()),
                driverId.toString()
        );
        redis.opsForHash().putAll(
                key(driverId),
                Map.of(
                        "accuracy", request.accuracyMeters().toString(),
                        "updatedAt", updatedAt.toString()
                )
        );
        redis.expire(key(driverId), staleAfter.multipliedBy(3));

        return new DriverLocationResponse(
                driverId,
                request.latitude(),
                request.longitude(),
                request.accuracyMeters(),
                recordedAt,
                updatedAt
        );
    }

    @Override
    public List<NearestDriverResponse> nearest(
            java.math.BigDecimal latitude,
            java.math.BigDecimal longitude,
            double radiusMeters,
            long limit
    ) {
        GeoResults<GeoLocation<String>> results = redis.opsForGeo().search(
                GEO_KEY,
                GeoReference.fromCoordinate(longitude.doubleValue(), latitude.doubleValue()),
                new Distance(radiusMeters / 1000, Metrics.KILOMETERS),
                GeoSearchCommandArgs.newGeoSearchArgs()
                        .includeDistance()
                        .sortAscending()
                        .limit(limit)
        );
        if (results == null) {
            return List.of();
        }

        Instant cutoff = Instant.now().minus(staleAfter);
        return results.getContent().stream()
                .map(result -> candidate(result, cutoff))
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<NearestDriverResponse> candidate(
            GeoResult<RedisGeoCommands.GeoLocation<String>> result,
            Instant cutoff
    ) {
        try {
            UUID driverId = UUID.fromString(result.getContent().getName());
            Map<Object, Object> metadata = redis.opsForHash().entries(key(driverId));
            Instant updatedAt = Instant.parse((String) metadata.get("updatedAt"));
            double accuracy = Double.parseDouble((String) metadata.get("accuracy"));
            if (updatedAt.isBefore(cutoff) || accuracy > maxAccuracy) {
                return Optional.empty();
            }
            return Optional.of(new NearestDriverResponse(
                    driverId,
                    Math.round(result.getDistance().getValue()),
                    updatedAt
            ));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private String key(UUID driverId) {
        return "tracking:driver:location:" + driverId;
    }
}
