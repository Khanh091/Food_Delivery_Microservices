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
    ADDRESS_COORDINATES_MISSING("DELIVERY_007", HttpStatus.UNPROCESSABLE_ENTITY, "Delivery address coordinates are required"),
    DELIVERY_NOT_SERVICEABLE("DELIVERY_008", HttpStatus.CONFLICT, "Delivery is unavailable for this address"),
    ROUTE_NOT_FOUND("DELIVERY_009", HttpStatus.CONFLICT, "No delivery route was found"),
    DELIVERY_PROVIDER_UNAVAILABLE("DELIVERY_010", HttpStatus.SERVICE_UNAVAILABLE, "Delivery routing is temporarily unavailable"),
    INTERNAL_SERVER_ERROR("DELIVERY_011", HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred"),
    CHECKOUT_LOCATION_NOT_FOUND("DELIVERY_012", HttpStatus.NOT_FOUND, "Checkout location was not found");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }
}
