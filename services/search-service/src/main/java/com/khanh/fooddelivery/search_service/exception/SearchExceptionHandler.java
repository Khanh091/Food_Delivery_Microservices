package com.khanh.fooddelivery.search_service.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
}
