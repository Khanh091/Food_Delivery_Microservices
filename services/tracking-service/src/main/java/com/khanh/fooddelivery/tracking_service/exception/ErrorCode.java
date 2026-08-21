package com.khanh.fooddelivery.tracking_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    INVALID_REQUEST("TRACKING_001", HttpStatus.BAD_REQUEST, "Invalid location request"),
    DRIVER_NOT_ACTIVE("TRACKING_002", HttpStatus.FORBIDDEN, "Driver profile is not active"),
    DRIVER_SERVICE_UNAVAILABLE("TRACKING_003", HttpStatus.SERVICE_UNAVAILABLE, "Driver status could not be verified"),
    LOCATION_ACCURACY_TOO_LOW("TRACKING_004", HttpStatus.UNPROCESSABLE_ENTITY, "Location accuracy is too low"),
    INTERNAL_SERVER_ERROR("TRACKING_005", HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }
}
