package com.khanh.fooddelivery.catalog_service.exception;

import java.time.Instant;

public record ErrorResponse(
        boolean success, String code, String message, String path, Instant timestamp) {
    static ErrorResponse of(ErrorCode code, String message, String path) {
        return new ErrorResponse(false, code.getCode(), message, path, Instant.now());
    }
}
