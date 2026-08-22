package com.khanh.fooddelivery.payment_service.exception;

import com.khanh.fooddelivery.payment_service.common.response.ApiResponse;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(PaymentException.class)
    ResponseEntity<ApiResponse<Void>> payment(PaymentException exception) {
        return ResponseEntity.status(exception.getStatus()).body(new ApiResponse<>(
                false, exception.getCode(), exception.getMessage(), null, Instant.now()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiResponse<Void>> status(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(new ApiResponse<>(
                false, "PAYMENT_" + exception.getStatusCode().value(), exception.getReason(), null, Instant.now()));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    ResponseEntity<ApiResponse<Void>> missingHeader(MissingRequestHeaderException exception) {
        return ResponseEntity.status(403).body(new ApiResponse<>(
                false, "PAYMENT_403", "Internal API credential is required", null, Instant.now()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> unexpected(Exception exception) {
        return ResponseEntity.internalServerError().body(new ApiResponse<>(
                false, "PAYMENT_500", "Payment operation failed", null, Instant.now()));
    }
}
