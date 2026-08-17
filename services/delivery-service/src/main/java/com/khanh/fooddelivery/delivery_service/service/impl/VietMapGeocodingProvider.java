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
        return request("/api/reverse/v4", uri -> uri.queryParam("lat", latitude.toPlainString())
                .queryParam("lng", longitude.toPlainString())
                .queryParam("display_type", properties.getDisplayType()), null);
    }

    @Override
    public List<GeocodedLocation> search(String query, BigDecimal latitude, BigDecimal longitude, int limit) {
        requireConfigured();
        try {
            String body = client().get().uri(uri -> {
                var builder = uri.path("/api/autocomplete/v4").queryParam("text", query)
                        .queryParam("display_type", properties.getDisplayType()).queryParam("limit", limit)
                        .queryParam("apikey", properties.getServiceKey());
                if (latitude != null && longitude != null) {
                    builder.queryParam("focus", latitude.toPlainString() + "," + longitude.toPlainString());
                }
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
        return request("/api/place/v4", uri -> uri.queryParam("refid", providerRefId), providerRefId);
    }

    private GeocodedLocation request(
            String path,
            java.util.function.Function<org.springframework.web.util.UriBuilder, org.springframework.web.util.UriBuilder> params,
            String refId) {
        requireConfigured();
        try {
            String body = client().get().uri(uri -> params.apply(uri.path(path)
                    .queryParam("apikey", properties.getServiceKey())).build())
                    .accept(MediaType.APPLICATION_JSON).retrieve().body(String.class);
            return mapDetail(body, objectMapper, refId);
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
        return providerError(exception.getStatusCode().value());
    }

    static AppException providerError(int status) {
        return new AppException(status == 404 ? ErrorCode.LOCATION_NOT_FOUND : ErrorCode.GEOCODING_PROVIDER_UNAVAILABLE);
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
                    results.add(location(refId, display, first(row, alternateFormat(row), "name", "address", "street"),
                            row, alternateFormat(row), null, null));
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
            JsonNode row = firstLocation(mapper.readTree(body));
            JsonNode oldFormat = alternateFormat(row);
            String display = first(row, oldFormat, "display", "address", "name");
            BigDecimal latitude = decimal(row, oldFormat, "lat");
            BigDecimal longitude = decimal(row, oldFormat, "lng");
            if (!StringUtils.hasText(display) || latitude == null || longitude == null) throw new AppException(ErrorCode.LOCATION_NOT_FOUND);
            return location(first(row, oldFormat, "ref_id", refId), display,
                    first(row, oldFormat, "name", "address", "street"), row, oldFormat, latitude, longitude);
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AppException(ErrorCode.GEOCODING_PROVIDER_UNAVAILABLE);
        }
    }

    private static GeocodedLocation location(String refId, String formattedAddress, String addressLine, JsonNode row, JsonNode oldFormat, BigDecimal latitude, BigDecimal longitude) {
        return new GeocodedLocation(refId, formattedAddress, addressLine,
                boundary(row, oldFormat, 2, "ward"), boundary(row, oldFormat, 1, "district"), boundary(row, oldFormat, 0, "city"), latitude, longitude);
    }

    private static String boundary(JsonNode row, JsonNode oldFormat, int type, String field) {
        String direct = first(row, oldFormat, field);
        if (StringUtils.hasText(direct)) return direct;
        String fromCurrent = boundaryValue(row, type);
        if (StringUtils.hasText(fromCurrent)) return fromCurrent;
        String fromOld = boundaryValue(oldFormat, type);
        if (StringUtils.hasText(fromOld)) return fromOld;
        return null;
    }

    private static JsonNode firstLocation(JsonNode response) {
        if (response.isObject()) return response;
        if (!response.isArray() || response.isEmpty()) throw new AppException(ErrorCode.LOCATION_NOT_FOUND);
        for (JsonNode item : response) if (item.isObject()) return item;
        throw new AppException(ErrorCode.LOCATION_NOT_FOUND);
    }

    private static JsonNode alternateFormat(JsonNode row) {
        JsonNode oldFormat = row.path("data_old");
        return oldFormat.isObject() ? oldFormat : null;
    }

    private static String boundaryValue(JsonNode row, int type) {
        if (row == null) return null;
        for (JsonNode boundary : row.path("boundaries")) {
            if (boundary.path("type").asInt(-1) == type) return first(boundary, "full_name", "name");
        }
        return null;
    }

    private static BigDecimal decimal(JsonNode node, JsonNode alternate, String field) {
        if (node.path(field).isNumber()) return node.path(field).decimalValue();
        return alternate != null && alternate.path(field).isNumber() ? alternate.path(field).decimalValue() : null;
    }

    private static String first(JsonNode node, String... fields) {
        for (String field : fields) {
            if (!StringUtils.hasText(field)) continue;
            String value = text(node, field);
            if (StringUtils.hasText(value)) return value;
        }
        return null;
    }
    private static String first(JsonNode node, JsonNode alternate, String... fields) {
        String current = first(node, fields);
        return StringUtils.hasText(current) || alternate == null ? current : first(alternate, fields);
    }
    private static String text(JsonNode node, String field) { String value = node.path(field).asText(null); return StringUtils.hasText(value) ? value : null; }
}
