package com.khanh.fooddelivery.delivery_service.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.khanh.fooddelivery.delivery_service.config.DeliveryVietMapProperties;
import com.khanh.fooddelivery.delivery_service.exception.AppException;
import com.khanh.fooddelivery.delivery_service.exception.ErrorCode;
import com.khanh.fooddelivery.delivery_service.service.GeocodingProvider;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class VietMapGeocodingProvider implements GeocodingProvider {
    private final DeliveryVietMapProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public GeocodedLocation reverseGeocode(BigDecimal latitude, BigDecimal longitude) {
        return request("/api/reverse/v3", uri -> uri.queryParam("lat", latitude.toPlainString())
                .queryParam("lng", longitude.toPlainString()), false);
    }

    @Override
    public List<GeocodedLocation> search(String query, BigDecimal latitude, BigDecimal longitude, int limit) {
        requireConfigured();
        try {
            String body = client().get().uri(uri -> {
                var builder = uri.path("/api/autocomplete/v3").queryParam("text", query)
                        .queryParam("display_type", properties.getDisplayType()).queryParam("limit", limit)
                        .queryParam("apikey", properties.getServiceKey());
                return builder.build();
            }).accept(MediaType.APPLICATION_JSON).retrieve().body(String.class);
            return mapAutocomplete(body, objectMapper);
        } catch (RestClientResponseException exception) {
            throw providerError(exception);
        } catch (RestClientException exception) {
            throw new AppException(ErrorCode.GEOCODING_PROVIDER_UNAVAILABLE);
        }
    }

    @Override
    public GeocodedLocation place(String providerRefId) {
        if (!StringUtils.hasText(providerRefId)) throw new AppException(ErrorCode.INVALID_REQUEST);
        return request("/api/place/v3", uri -> uri.queryParam("refid", providerRefId), true);
    }

    private GeocodedLocation request(String path, java.util.function.Function<org.springframework.web.util.UriBuilder, org.springframework.web.util.UriBuilder> params, boolean place) {
        requireConfigured();
        try {
            String body = client().get().uri(uri -> params.apply(uri.path(path)
                    .queryParam("apikey", properties.getServiceKey())).build())
                    .accept(MediaType.APPLICATION_JSON).retrieve().body(String.class);
            return mapDetail(body, objectMapper, place ? null : null);
        } catch (RestClientResponseException exception) {
            throw providerError(exception);
        } catch (RestClientException exception) {
            throw new AppException(ErrorCode.GEOCODING_PROVIDER_UNAVAILABLE);
        }
    }

    private RestClient client() { return RestClient.create(properties.getApiBaseUrl()); }

    private void requireConfigured() {
        if (!StringUtils.hasText(properties.getServiceKey())) throw new AppException(ErrorCode.GEOCODING_NOT_CONFIGURED);
    }

    private AppException providerError(RestClientResponseException exception) {
        if (exception.getStatusCode().value() == 404) return new AppException(ErrorCode.LOCATION_NOT_FOUND);
        return new AppException(ErrorCode.GEOCODING_PROVIDER_UNAVAILABLE);
    }

    static List<GeocodedLocation> mapAutocomplete(String body, ObjectMapper mapper) {
        try {
            JsonNode rows = mapper.readTree(body);
            if (!rows.isArray()) throw new AppException(ErrorCode.GEOCODING_PROVIDER_UNAVAILABLE);
            List<GeocodedLocation> results = new ArrayList<>();
            for (JsonNode row : rows) {
                String refId = text(row, "ref_id");
                String display = first(row, "display", "address", "name");
                if (StringUtils.hasText(refId) && StringUtils.hasText(display)) {
                    results.add(location(refId, display, first(row, "name", "address"), row, null, null));
                }
            }
            return results;
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AppException(ErrorCode.GEOCODING_PROVIDER_UNAVAILABLE);
        }
    }

    static GeocodedLocation mapDetail(String body, ObjectMapper mapper, String refId) {
        try {
            JsonNode row = mapper.readTree(body);
            String display = first(row, "display", "address", "name");
            BigDecimal latitude = decimal(row, "lat");
            BigDecimal longitude = decimal(row, "lng");
            if (!StringUtils.hasText(display) || latitude == null || longitude == null) throw new AppException(ErrorCode.LOCATION_NOT_FOUND);
            return location(first(row, "ref_id", refId), display, first(row, "address", "name"), row, latitude, longitude);
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AppException(ErrorCode.GEOCODING_PROVIDER_UNAVAILABLE);
        }
    }

    private static GeocodedLocation location(String refId, String formattedAddress, String addressLine, JsonNode row, BigDecimal latitude, BigDecimal longitude) {
        return new GeocodedLocation(refId, formattedAddress, addressLine,
                boundary(row, 2, "ward"), boundary(row, 1, "district"), boundary(row, 0, "city"), latitude, longitude);
    }

    private static String boundary(JsonNode row, int type, String field) {
        String direct = text(row, field);
        if (StringUtils.hasText(direct)) return direct;
        for (JsonNode boundary : row.path("boundaries")) if (boundary.path("type").asInt(-1) == type) return first(boundary, "full_name", "name");
        return null;
    }

    private static BigDecimal decimal(JsonNode node, String field) { return node.path(field).isNumber() ? node.path(field).decimalValue() : null; }
    private static String first(JsonNode node, String... fields) { for (String field : fields) { String value = text(node, field); if (StringUtils.hasText(value)) return value; } return null; }
    private static String text(JsonNode node, String field) { String value = node.path(field).asText(null); return StringUtils.hasText(value) ? value : null; }
}
