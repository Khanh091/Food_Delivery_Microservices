package com.khanh.fooddelivery.catalog_service.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(AppException.class)
    ResponseEntity<ErrorResponse> app(AppException e, HttpServletRequest r) {
        return out(e.getErrorCode(), e.getMessage(), r);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> conflict(
            DataIntegrityViolationException e, HttpServletRequest r) {
        return out(ErrorCode.DATA_CONFLICT, ErrorCode.DATA_CONFLICT.getDefaultMessage(), r);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> denied(AccessDeniedException e, HttpServletRequest r) {
        return out(ErrorCode.ACCESS_DENIED, ErrorCode.ACCESS_DENIED.getDefaultMessage(), r);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(
            MethodArgumentNotValidException e, HttpServletRequest r) {
        String message =
                e.getBindingResult().getFieldErrors().stream()
                        .map(FieldError::getField)
                        .distinct()
                        .sorted()
                        .reduce((left, right) -> left + ", " + right)
                        .map(fields -> "Invalid value for: " + fields)
                        .orElse(ErrorCode.INVALID_REQUEST.getDefaultMessage());
        return out(ErrorCode.INVALID_REQUEST, message, r);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception e, HttpServletRequest r) {
        log.error("Unhandled catalog request failure path={}", r.getRequestURI(), e);
        return out(
                ErrorCode.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage(),
                r);
    }

    private ResponseEntity<ErrorResponse> out(
            ErrorCode code, String message, HttpServletRequest request) {
        return ResponseEntity.status(code.getHttpStatus())
                .body(ErrorResponse.of(code, message, request.getRequestURI()));
    }
}
