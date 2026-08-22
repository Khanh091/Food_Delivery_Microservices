package com.khanh.fooddelivery.tracking_service.service.impl;

import com.khanh.fooddelivery.tracking_service.client.DriverServiceClient;
import com.khanh.fooddelivery.tracking_service.client.dto.response.DriverProfileResponse;
import com.khanh.fooddelivery.tracking_service.dto.request.DriverLocationUpdateRequest;
import com.khanh.fooddelivery.tracking_service.dto.response.DriverLocationResponse;
import com.khanh.fooddelivery.tracking_service.dto.response.NearestDriverResponse;
import com.khanh.fooddelivery.tracking_service.exception.AppException;
import com.khanh.fooddelivery.tracking_service.exception.ErrorCode;
import com.khanh.fooddelivery.tracking_service.service.DriverLocationService;
import feign.FeignException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
            String authorization,
            DriverLocationUpdateRequest request
    ) {
        DriverProfileResponse profile = currentProfile(authorization);
        UUID driverId = profile.userId();
        if (request.accuracyMeters() > maxAccuracy) {
            throw new AppException(
                    ErrorCode.LOCATION_ACCURACY_TOO_LOW,
                    "Location accuracy is too low"
            );
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
                        "recordedAt", recordedAt.toString(),
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

    private DriverProfileResponse currentProfile(String authorization) {
        try {
            DriverProfileResponse profile = drivers.profile(authorization);
            if (profile == null
                    || profile.userId() == null
                    || !"ACTIVE".equalsIgnoreCase(profile.status())) {
                throw new AppException(
                        ErrorCode.DRIVER_NOT_ACTIVE,
                        "Driver profile must be ACTIVE to upload location"
                );
            }
            return profile;
        } catch (FeignException.NotFound exception) {
            throw new AppException(
                    ErrorCode.DRIVER_NOT_ACTIVE,
                    "Driver profile must be ACTIVE to upload location"
            );
        } catch (FeignException exception) {
            throw new AppException(
                    ErrorCode.DRIVER_SERVICE_UNAVAILABLE,
                    "Driver status could not be verified"
            );
        }
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

        Instant now = Instant.now();
        Instant cutoff = now.minus(staleAfter);
        log.debug(
                "Nearest driver search lat={} lon={} radiusMeters={} limit={} geoCandidates={}",
                latitude,
                longitude,
                radiusMeters,
                limit,
                results.getContent().size()
        );
        List<NearestDriverResponse> eligible = results.getContent().stream()
                .map(result -> candidate(result, cutoff, now))
                .flatMap(Optional::stream)
                .toList();
        if (eligible.isEmpty()) {
            log.info(
                    "Nearest driver search returned no eligible candidates geoCandidates={} staleAfterSeconds={} maxAccuracyMeters={}",
                    results.getContent().size(),
                    staleAfter.toSeconds(),
                    maxAccuracy
            );
        }
        return eligible;
    }

    private Optional<NearestDriverResponse> candidate(
            GeoResult<RedisGeoCommands.GeoLocation<String>> result,
            Instant cutoff,
            Instant now
    ) {
        String member = result.getContent().getName();
        UUID driverId;
        try {
            driverId = UUID.fromString(member);
        } catch (IllegalArgumentException exception) {
            log.warn("Nearest driver candidate excluded reason=invalid_geo_member member={}", member);
            return Optional.empty();
        }

        Map<Object, Object> metadata;
        try {
            metadata = redis.opsForHash().entries(key(driverId));
        } catch (RuntimeException exception) {
            log.warn(
                    "Nearest driver candidate excluded driverId={} reason=metadata_lookup_failed exception={}",
                    driverId,
                    exception.getClass().getSimpleName()
            );
            return Optional.empty();
        }

        if (metadata == null) {
            log.debug(
                    "Nearest driver candidate excluded driverId={} reason=missing_location_metadata metadataKey={}",
                    driverId,
                    key(driverId)
            );
            return Optional.empty();
        }
        Object updatedAtValue = metadata.get("updatedAt");
        Object accuracyValue = metadata.get("accuracy");
        if (updatedAtValue == null || accuracyValue == null) {
            log.debug(
                    "Nearest driver candidate excluded driverId={} reason=missing_location_metadata metadataKey={}",
                    driverId,
                    key(driverId)
            );
            return Optional.empty();
        }

        Instant updatedAt;
        try {
            updatedAt = Instant.parse(updatedAtValue.toString());
        } catch (RuntimeException exception) {
            log.debug(
                    "Nearest driver candidate excluded driverId={} reason=invalid_updated_at value={}",
                    driverId,
                    updatedAtValue
            );
            return Optional.empty();
        }

        double accuracy;
        try {
            accuracy = Double.parseDouble(accuracyValue.toString());
        } catch (RuntimeException exception) {
            log.debug(
                    "Nearest driver candidate excluded driverId={} reason=invalid_accuracy value={}",
                    driverId,
                    accuracyValue
            );
            return Optional.empty();
        }

        if (updatedAt.isBefore(cutoff)) {
            log.debug(
                    "Nearest driver candidate excluded driverId={} reason=stale ageSeconds={}",
                    driverId,
                    Math.max(0, Duration.between(updatedAt, now).toSeconds())
            );
            return Optional.empty();
        }
        if (accuracy > maxAccuracy) {
            log.debug(
                    "Nearest driver candidate excluded driverId={} reason=accuracy_too_low accuracyMeters={} maxAccuracyMeters={}",
                    driverId,
                    accuracy,
                    maxAccuracy
            );
            return Optional.empty();
        }

        log.debug(
                "Nearest driver candidate eligible driverId={} distanceMeters={} ageSeconds={} accuracyMeters={}",
                driverId,
                distanceMeters(result.getDistance()),
                Math.max(0, Duration.between(updatedAt, now).toSeconds()),
                accuracy
        );
        return Optional.of(new NearestDriverResponse(
                driverId,
                distanceMeters(result.getDistance()),
                updatedAt
        ));
    }

    private long distanceMeters(Distance distance) {
        return Math.round(distance.in(Metrics.KILOMETERS).getValue() * 1000d);
    }

    private String key(UUID driverId) {
        return "tracking:driver:location:" + driverId;
    }
}
