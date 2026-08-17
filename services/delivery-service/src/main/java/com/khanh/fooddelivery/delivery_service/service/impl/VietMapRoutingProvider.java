package com.khanh.fooddelivery.delivery_service.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.khanh.fooddelivery.delivery_service.config.DeliveryRoutingProperties;
import com.khanh.fooddelivery.delivery_service.config.DeliveryVietMapProperties;
import com.khanh.fooddelivery.delivery_service.exception.AppException;
import com.khanh.fooddelivery.delivery_service.exception.ErrorCode;
import com.khanh.fooddelivery.delivery_service.service.RoutingProvider;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

/** VietMap Route v4 adapter. VietMap accepts route points as latitude,longitude. */
@Component
@RequiredArgsConstructor
public class VietMapRoutingProvider implements RoutingProvider {
    private final DeliveryVietMapProperties vietMapProperties;
    private final DeliveryRoutingProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public Route calculateRoute(
            BigDecimal originLatitude, BigDecimal originLongitude,
            BigDecimal destinationLatitude, BigDecimal destinationLongitude) {
        if (!StringUtils.hasText(vietMapProperties.getServiceKey())) {
            throw new AppException(ErrorCode.DELIVERY_PROVIDER_UNAVAILABLE);
        }
        try {
            String body = RestClient.create(vietMapProperties.getApiBaseUrl()).get()
                    .uri(routeUri(vietMapProperties.getApiBaseUrl(), vietMapProperties.getServiceKey(),
                            properties.getVehicle(), originLatitude, originLongitude,
                            destinationLatitude, destinationLongitude))
                    .retrieve()
                    .body(String.class);
            return map(body, objectMapper);
        } catch (RestClientResponseException exception) {
            throw mapHttpStatus(exception.getStatusCode().value());
        } catch (RestClientException exception) {
            throw new AppException(ErrorCode.DELIVERY_PROVIDER_UNAVAILABLE);
        }
    }

    static URI routeUri(
            String baseUrl, String apiKey, String vehicle,
            BigDecimal originLatitude, BigDecimal originLongitude,
            BigDecimal destinationLatitude, BigDecimal destinationLongitude) {
        return UriComponentsBuilder.fromUriString(baseUrl)
                .path("/api/route/v4")
                .queryParam("apikey", apiKey)
                .queryParam("point", originLatitude.toPlainString() + "," + originLongitude.toPlainString())
                .queryParam("point", destinationLatitude.toPlainString() + "," + destinationLongitude.toPlainString())
                .queryParam("points_encoded", true)
                .queryParam("vehicle", vehicle)
                .build(true)
                .toUri();
    }

    static AppException mapHttpStatus(int status) {
        return new AppException(status == 404 ? ErrorCode.ROUTE_NOT_FOUND : ErrorCode.DELIVERY_PROVIDER_UNAVAILABLE);
    }

    static Route map(String body, ObjectMapper objectMapper) {
        try {
            JsonNode root = objectMapper.readTree(body);
            String code = root.path("code").asText("");
            if ("ZERO_RESULTS".equalsIgnoreCase(code)) {
                throw new AppException(ErrorCode.ROUTE_NOT_FOUND);
            }
            if (!"OK".equalsIgnoreCase(code)) {
                throw new AppException(ErrorCode.DELIVERY_PROVIDER_UNAVAILABLE);
            }
            JsonNode paths = root.path("paths");
            if (!paths.isArray() || paths.isEmpty()) {
                throw new AppException(ErrorCode.ROUTE_NOT_FOUND);
            }
            JsonNode path = paths.get(0);
            JsonNode distanceNode = path.path("distance");
            JsonNode durationNode = path.path("time");
            if (!distanceNode.isNumber() || !durationNode.isNumber()) {
                throw new AppException(ErrorCode.DELIVERY_PROVIDER_UNAVAILABLE);
            }
            BigDecimal distance = distanceNode.decimalValue();
            BigDecimal durationMillis = durationNode.decimalValue();
            if (distance.signum() < 0 || durationMillis.signum() < 0) {
                throw new AppException(ErrorCode.ROUTE_NOT_FOUND);
            }
            long distanceMeters = distance.setScale(0, RoundingMode.HALF_UP).longValueExact();
            long durationSeconds = durationMillis.divide(BigDecimal.valueOf(1000), 0, RoundingMode.CEILING).longValueExact();
            return new Route(distanceMeters, durationSeconds);
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AppException(ErrorCode.DELIVERY_PROVIDER_UNAVAILABLE);
        }
    }
}
