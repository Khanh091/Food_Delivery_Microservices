package com.khanh.fooddelivery.delivery_service.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khanh.fooddelivery.delivery_service.config.DeliveryGeocodingProperties;
import com.khanh.fooddelivery.delivery_service.exception.AppException;
import com.khanh.fooddelivery.delivery_service.exception.ErrorCode;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class GoogleGeocodingProviderTests {
    @Test
    void reportsNotConfiguredWithoutCallingAnExternalProvider() {
        DeliveryGeocodingProperties properties = new DeliveryGeocodingProperties();
        properties.setApiKey("");

        assertThatThrownBy(() -> new GoogleGeocodingProvider(properties, new ObjectMapper())
                .reverseGeocode(BigDecimal.TEN, BigDecimal.ONE))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).getErrorCode())
                .isEqualTo(ErrorCode.GEOCODING_NOT_CONFIGURED);
    }
}
