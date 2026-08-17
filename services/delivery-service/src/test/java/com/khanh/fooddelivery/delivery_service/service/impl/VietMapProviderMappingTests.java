package com.khanh.fooddelivery.delivery_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khanh.fooddelivery.delivery_service.exception.AppException;
import com.khanh.fooddelivery.delivery_service.exception.ErrorCode;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class VietMapProviderMappingTests {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapsVietnameseAutocompleteWithoutInventingCoordinates() {
        var results = VietMapGeocodingProvider.mapAutocomplete("""
                [{"ref_id":"auto:123","name":"136 Nguyễn Phong Sắc","display":"136 Nguyễn Phong Sắc Phường Dịch Vọng,Quận Cầu Giấy,Thành Phố Hà Nội","boundaries":[{"type":2,"full_name":"Phường Dịch Vọng"},{"type":1,"full_name":"Quận Cầu Giấy"},{"type":0,"full_name":"Thành Phố Hà Nội"}]}]
                """, objectMapper);
        assertThat(results).singleElement().satisfies(result -> {
            assertThat(result.providerRefId()).isEqualTo("auto:123");
            assertThat(result.latitude()).isNull();
            assertThat(result.longitude()).isNull();
            assertThat(result.district()).isEqualTo("Quận Cầu Giấy");
        });
    }

    @Test
    void mapsPlaceAndReverseDetailsWithVietnameseBoundaries() {
        var result = VietMapGeocodingProvider.mapDetail("""
                {"display":"136 Nguyễn Phong Sắc,Phường Dịch Vọng,Quận Cầu Giấy,Thành Phố Hà Nội","address":"136 Nguyễn Phong Sắc","ward":"Phường Dịch Vọng","district":"Quận Cầu Giấy","city":"Thành Phố Hà Nội","lat":21.0412956,"lng":105.7906442}
                """, objectMapper, "auto:123");
        assertThat(result.formattedAddress()).contains("Nguyễn Phong Sắc");
        assertThat(result.latitude()).isEqualByComparingTo("21.0412956");
        assertThat(result.longitude()).isEqualByComparingTo("105.7906442");
        assertThat(result.ward()).isEqualTo("Phường Dịch Vọng");
    }

    @Test
    void acceptsEmptyAutocompleteAsNoSuggestions() {
        assertThat(VietMapGeocodingProvider.mapAutocomplete("[]", objectMapper)).isEmpty();
    }

    @Test
    void rejectsMalformedLocationDetail() {
        assertThatThrownBy(() -> VietMapGeocodingProvider.mapDetail("{}", objectMapper, "auto:123"))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).getErrorCode())
                .isEqualTo(ErrorCode.LOCATION_NOT_FOUND);
    }

    @Test
    void mapsVietMapRouteV4DistanceAndMillisecondsToSeconds() {
        var route = VietMapRoutingProvider.map("{\"code\":\"OK\",\"paths\":[{\"distance\":1532.7,\"time\":412001}]}", objectMapper);
        assertThat(route.distanceMeters()).isEqualTo(1533);
        assertThat(route.durationSeconds()).isEqualTo(413);
    }

    @Test
    void buildsRouteV4RequestInLatitudeLongitudeOrder() {
        var uri = VietMapRoutingProvider.routeUri("https://maps.vietmap.vn", "test-key", "car",
                new BigDecimal("21.0283"), new BigDecimal("105.854"),
                new BigDecimal("21.038"), new BigDecimal("105.864"));
        assertThat(uri.toString()).contains("/api/route/v4")
                .contains("point=21.0283,105.854")
                .contains("point=21.038,105.864")
                .contains("vehicle=car");
    }

    @Test
    void mapsNoRouteAndMalformedRouteSafely() {
        assertThatThrownBy(() -> VietMapRoutingProvider.map("{\"code\":\"ZERO_RESULTS\",\"paths\":[]}", objectMapper))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).getErrorCode())
                .isEqualTo(ErrorCode.ROUTE_NOT_FOUND);
        assertThatThrownBy(() -> VietMapRoutingProvider.map("{\"code\":\"OK\",\"paths\":[{}]}", objectMapper))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).getErrorCode())
                .isEqualTo(ErrorCode.DELIVERY_PROVIDER_UNAVAILABLE);
    }

    @Test
    void mapsRouteProviderAuthRateLimitAndServerErrorsToSafeBusinessError() {
        for (int status : new int[] {401, 403, 429, 500}) {
            assertThat(VietMapRoutingProvider.mapHttpStatus(status).getErrorCode())
                    .as("status %s", status)
                    .isEqualTo(ErrorCode.DELIVERY_PROVIDER_UNAVAILABLE);
        }
        assertThat(VietMapRoutingProvider.mapHttpStatus(404).getErrorCode()).isEqualTo(ErrorCode.ROUTE_NOT_FOUND);
    }
}
