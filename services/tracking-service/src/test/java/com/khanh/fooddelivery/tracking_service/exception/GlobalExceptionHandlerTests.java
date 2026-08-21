package com.khanh.fooddelivery.tracking_service.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void inactive_driver_is_returned_as_standard_forbidden_error() {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "PUT",
                "/api/v1/tracking/drivers/me/location"
        );

        ResponseEntity<ErrorResponse> response = handler.app(
                new AppException(
                        ErrorCode.DRIVER_NOT_ACTIVE,
                        "Driver profile must be ACTIVE to upload location"
                ),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody())
                .extracting(ErrorResponse::code, ErrorResponse::message, ErrorResponse::path)
                .containsExactly(
                        "TRACKING_002",
                        "Driver profile must be ACTIVE to upload location",
                        "/api/v1/tracking/drivers/me/location"
                );
    }
}
