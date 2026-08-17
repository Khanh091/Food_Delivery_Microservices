package com.khanh.fooddelivery.delivery_service.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.khanh.fooddelivery.delivery_service.config.DeliveryGeocodingProperties;
import com.khanh.fooddelivery.delivery_service.config.DeliveryRoutingProperties;
import com.khanh.fooddelivery.delivery_service.exception.AppException;
import com.khanh.fooddelivery.delivery_service.exception.ErrorCode;
import com.khanh.fooddelivery.delivery_service.service.RoutingProvider;
import java.math.BigDecimal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class GoogleRoutesProvider implements RoutingProvider {
    private final DeliveryRoutingProperties routingProperties;
    private final DeliveryGeocodingProperties geocodingProperties;
    private final ObjectMapper objectMapper;

    @Override
    public Route calculateRoute(
            BigDecimal originLatitude, BigDecimal originLongitude,
            BigDecimal destinationLatitude, BigDecimal destinationLongitude) {
        if (!"google".equalsIgnoreCase(routingProperties.getProvider())
                || !StringUtils.hasText(geocodingProperties.getApiKey())) {
            throw new AppException(ErrorCode.DELIVERY_PROVIDER_UNAVAILABLE);
        }
        try {
            String body = RestClient.create(routingProperties.getGoogleBaseUrl()).post()
                    .uri("/directions/v2:computeRoutes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Goog-Api-Key", geocodingProperties.getApiKey())
                    .header("X-Goog-FieldMask", "routes.distanceMeters,routes.duration")
                    .body(Map.of(
                            "origin", waypoint(originLatitude, originLongitude),
                            "destination", waypoint(destinationLatitude, destinationLongitude),
                            "travelMode", "DRIVE",
                            "routingPreference", "TRAFFIC_UNAWARE"))
                    .retrieve().body(String.class);
            return map(body);
        } catch (AppException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new AppException(ErrorCode.DELIVERY_PROVIDER_UNAVAILABLE);
        }
    }

    private Map<String, Object> waypoint(BigDecimal latitude, BigDecimal longitude) {
        return Map.of("location", Map.of("latLng", Map.of("latitude", latitude, "longitude", longitude)));
    }

    private Route map(String body) {
        try {
            JsonNode routes = objectMapper.readTree(body).path("routes");
            if (!routes.isArray() || routes.isEmpty()) throw new AppException(ErrorCode.ROUTE_NOT_FOUND);
            JsonNode route = routes.get(0);
            long distanceMeters = route.path("distanceMeters").asLong(-1);
            long durationSeconds = parseDuration(route.path("duration").asText());
            if (distanceMeters < 0 || durationSeconds < 0) throw new AppException(ErrorCode.ROUTE_NOT_FOUND);
            return new Route(distanceMeters, durationSeconds);
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AppException(ErrorCode.DELIVERY_PROVIDER_UNAVAILABLE);
        }
    }

    private long parseDuration(String value) {
        if (value == null || !value.matches("\\d+(?:\\.\\d+)?s")) return -1;
        return new BigDecimal(value.substring(0, value.length() - 1)).longValue();
    }
}
