package com.khanh.fooddelivery.user_service.exception;

import java.time.Instant;

public record ErrorResponse(
        boolean success,
        String code,
        String message,
        Instant timestamp,
        String path
) {
    public static ErrorResponse of(
            ErrorCode errorCode,
            String message,
            String path
    ) {
        return new ErrorResponse(
                false,
                errorCode.getCode(),
                message,
                Instant.now(),
                path
        );
    }
}
