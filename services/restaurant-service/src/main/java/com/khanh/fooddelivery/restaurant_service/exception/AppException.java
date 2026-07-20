package com.khanh.fooddelivery.restaurant_service.exception;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {
    private final ErrorCode errorCode;

    public AppException(ErrorCode code) {
        super(code.getDefaultMessage());
        this.errorCode = code;
    }

    public AppException(ErrorCode code, String message) {
        super(message);
        this.errorCode = code;
    }
}
