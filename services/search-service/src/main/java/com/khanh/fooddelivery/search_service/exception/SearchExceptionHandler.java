package com.khanh.fooddelivery.search_service.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class SearchExceptionHandler {
    @ExceptionHandler(SearchApiException.class)
    ResponseEntity<SearchErrorResponse> handle(SearchApiException exception, HttpServletRequest request) {
        SearchErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus())
                .body(
                        new SearchErrorResponse(
                                errorCode.getCode(),
                                errorCode.getMessage(),
                                request.getRequestURI(),
                                Instant.now()));
    }

    @ExceptionHandler({ConstraintViolationException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<SearchErrorResponse> handleInvalidRequest(
            RuntimeException exception, HttpServletRequest request) {
        return ResponseEntity.status(SearchErrorCode.INVALID_REQUEST.getStatus())
                .body(
                        new SearchErrorResponse(
                                SearchErrorCode.INVALID_REQUEST.getCode(),
                                SearchErrorCode.INVALID_REQUEST.getMessage(),
                                request.getRequestURI(),
                                Instant.now()));
    }
}
