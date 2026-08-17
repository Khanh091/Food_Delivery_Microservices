package com.khanh.fooddelivery.delivery_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.delivery_service.dto.request.ReverseGeocodeRequest;
import com.khanh.fooddelivery.delivery_service.dto.response.ReverseGeocodeResponse;
import com.khanh.fooddelivery.delivery_service.exception.AppException;
import com.khanh.fooddelivery.delivery_service.exception.ErrorCode;
import com.khanh.fooddelivery.delivery_service.service.GeocodingProvider;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReverseGeocodingServiceImplTests {
    @Mock
    private GeocodingProvider provider;

    @Test
    void mapsProviderResultWithoutExposingProviderShape() {
        when(provider.reverseGeocode(new BigDecimal("10.776889"), new BigDecimal("106.700806")))
                .thenReturn(new GeocodingProvider.GeocodedLocation(null, "12 Nguyen Trai, Ha Noi", "12 Nguyen Trai",
                        "Thanh Xuan Trung", "Thanh Xuan", "Ha Noi", null, null));

        ReverseGeocodeResponse response = new ReverseGeocodingServiceImpl(provider)
                .reverseGeocode(new ReverseGeocodeRequest(new BigDecimal("10.776889"), new BigDecimal("106.700806")));

        assertThat(response.formattedAddress()).isEqualTo("12 Nguyen Trai, Ha Noi");
        assertThat(response.latitude()).isEqualByComparingTo("10.776889");
        assertThat(response.longitude()).isEqualByComparingTo("106.700806");
    }

    @Test
    void rejectsCoordinatesOutsideGeographicRange() {
        assertThatThrownBy(() -> new ReverseGeocodingServiceImpl(provider)
                .reverseGeocode(new ReverseGeocodeRequest(new BigDecimal("91"), new BigDecimal("106"))))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    void rejectsLongitudeOutsideGeographicRange() {
        assertThatThrownBy(() -> new ReverseGeocodingServiceImpl(provider)
                .reverseGeocode(new ReverseGeocodeRequest(new BigDecimal("10"), new BigDecimal("181"))))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    void preservesProviderUnavailableError() {
        when(provider.reverseGeocode(new BigDecimal("10"), new BigDecimal("106")))
                .thenThrow(new AppException(ErrorCode.GEOCODING_PROVIDER_UNAVAILABLE));

        assertThatThrownBy(() -> new ReverseGeocodingServiceImpl(provider)
                .reverseGeocode(new ReverseGeocodeRequest(new BigDecimal("10"), new BigDecimal("106"))))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).getErrorCode())
                .isEqualTo(ErrorCode.GEOCODING_PROVIDER_UNAVAILABLE);
    }
}
