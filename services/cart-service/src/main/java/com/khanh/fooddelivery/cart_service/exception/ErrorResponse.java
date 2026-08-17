package com.khanh.fooddelivery.cart_service.exception;

import java.time.Instant;

public record ErrorResponse(String code, String message, String path, Instant timestamp) {
    public static ErrorResponse of(ErrorCode errorCode, String message, String path) {
        return new ErrorResponse(errorCode.getCode(), message, path, Instant.now());
    }
}
