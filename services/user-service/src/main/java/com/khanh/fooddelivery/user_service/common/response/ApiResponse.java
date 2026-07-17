package com.khanh.fooddelivery.user_service.common.response;

import java.time.Instant;

public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        Instant timestamp
) {
    private static final String SUCCESS_CODE = "SUCCESS";
    private static final String DEFAULT_MESSAGE = "Request processed successfully";

    public static <T> ApiResponse<T> success(T data) {
        return success(DEFAULT_MESSAGE, data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(
                true,
                SUCCESS_CODE,
                message,
                data,
                Instant.now()
        );
    }

    public static ApiResponse<Void> success(String message) {
        return success(message, null);
    }
}
