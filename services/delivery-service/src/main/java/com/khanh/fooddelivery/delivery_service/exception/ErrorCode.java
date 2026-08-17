package com.khanh.fooddelivery.delivery_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    INVALID_REQUEST("DELIVERY_001", HttpStatus.BAD_REQUEST, "Invalid location request"),
    UNAUTHENTICATED("DELIVERY_002", HttpStatus.UNAUTHORIZED, "Authentication is required"),
    ACCESS_DENIED("DELIVERY_003", HttpStatus.FORBIDDEN, "Access denied"),
    GEOCODING_NOT_CONFIGURED("DELIVERY_004", HttpStatus.SERVICE_UNAVAILABLE, "Location lookup is not configured"),
    GEOCODING_PROVIDER_UNAVAILABLE("DELIVERY_005", HttpStatus.SERVICE_UNAVAILABLE, "Location lookup is temporarily unavailable"),
    LOCATION_NOT_FOUND("DELIVERY_006", HttpStatus.UNPROCESSABLE_ENTITY, "No address was found for this location"),
    INTERNAL_SERVER_ERROR("DELIVERY_007", HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }
}
