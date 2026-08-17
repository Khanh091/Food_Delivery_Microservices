package com.khanh.fooddelivery.delivery_service.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.khanh.fooddelivery.delivery_service.config.DeliveryGeocodingProperties;
import com.khanh.fooddelivery.delivery_service.exception.AppException;
import com.khanh.fooddelivery.delivery_service.exception.ErrorCode;
import com.khanh.fooddelivery.delivery_service.service.GeocodingProvider;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class GoogleGeocodingProvider implements GeocodingProvider {
    private final DeliveryGeocodingProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public GeocodedLocation reverseGeocode(BigDecimal latitude, BigDecimal longitude) {
        if (!"google".equalsIgnoreCase(properties.getProvider())) {
            throw new AppException(ErrorCode.GEOCODING_NOT_CONFIGURED);
        }
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new AppException(ErrorCode.GEOCODING_NOT_CONFIGURED);
        }
        try {
            String body = RestClient.create(properties.getGoogleBaseUrl()).get()
                    .uri(uri -> uri.path("/maps/api/geocode/json")
                            .queryParam("latlng", latitude.toPlainString() + "," + longitude.toPlainString())
                            .queryParam("key", properties.getApiKey()).build())
                    .accept(MediaType.APPLICATION_JSON).retrieve().body(String.class);
            return map(body);
        } catch (AppException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new AppException(ErrorCode.GEOCODING_PROVIDER_UNAVAILABLE);
        }
    }

    private GeocodedLocation map(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            String status = root.path("status").asText();
            if ("ZERO_RESULTS".equals(status)) throw new AppException(ErrorCode.LOCATION_NOT_FOUND);
            if (!"OK".equals(status) || !root.path("results").isArray() || root.path("results").isEmpty()) {
                throw new AppException(ErrorCode.GEOCODING_PROVIDER_UNAVAILABLE);
            }
            JsonNode result = root.path("results").get(0);
            return new GeocodedLocation(result.path("formatted_address").asText(), addressLine(result),
                    component(result, "administrative_area_level_3"), component(result, "administrative_area_level_2"),
                    component(result, "administrative_area_level_1"), null, null);
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AppException(ErrorCode.GEOCODING_PROVIDER_UNAVAILABLE);
        }
    }

    private String component(JsonNode result, String type) {
        for (JsonNode component : result.path("address_components")) {
            for (JsonNode componentType : component.path("types")) {
                if (type.equals(componentType.asText())) return component.path("long_name").asText();
            }
        }
        return null;
    }

    private String addressLine(JsonNode result) {
        String number = component(result, "street_number");
        String route = component(result, "route");
        if (!StringUtils.hasText(number)) return route;
        if (!StringUtils.hasText(route)) return number;
        return number + " " + route;
    }
}
