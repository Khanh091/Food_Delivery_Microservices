package com.khanh.fooddelivery.catalog_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    INVALID_REQUEST("CATALOG_001", HttpStatus.BAD_REQUEST, "Invalid request"),
    ACCESS_DENIED("CATALOG_002", HttpStatus.FORBIDDEN, "Access denied"),
    UNAUTHENTICATED("CATALOG_003", HttpStatus.UNAUTHORIZED, "Authentication is required"),
    DATA_CONFLICT("CATALOG_004", HttpStatus.CONFLICT, "Data conflict"),
    INTERNAL_SERVER_ERROR(
            "CATALOG_005", HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }
}
