package com.khanh.fooddelivery.tracking_service.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    ResponseEntity<ErrorResponse> app(AppException exception, HttpServletRequest request) {
        return response(exception.getErrorCode(), exception.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> denied(AccessDeniedException exception, HttpServletRequest request) {
        return response(ErrorCode.DRIVER_NOT_ACTIVE, "Driver is not allowed to upload location", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorResponse> invalid(IllegalArgumentException exception, HttpServletRequest request) {
        return response(ErrorCode.INVALID_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception exception, HttpServletRequest request) {
        log.error("Unhandled tracking request failure at {}: {}", request.getRequestURI(), exception.getMessage(), exception);
        return response(
                ErrorCode.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage(),
                request
        );
    }

    private ResponseEntity<ErrorResponse> response(
            ErrorCode errorCode,
            String message,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode, message, request.getRequestURI()));
    }
}
