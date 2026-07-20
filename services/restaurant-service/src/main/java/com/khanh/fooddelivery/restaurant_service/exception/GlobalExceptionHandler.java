package com.khanh.fooddelivery.restaurant_service.exception;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException.Reference;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AppException.class)
    ResponseEntity<ErrorResponse> app(AppException e, HttpServletRequest r) {
        return out(e.getErrorCode(), e.getMessage(), r);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(
            MethodArgumentNotValidException e, HttpServletRequest r) {
        String m =
                e.getBindingResult().getFieldErrors().stream()
                        .sorted(Comparator.comparing(FieldError::getField))
                        .map(x -> x.getField() + ": " + x.getDefaultMessage())
                        .collect(Collectors.joining("; "));
        return out(ErrorCode.COMMON_VALIDATION_ERROR, m, r);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorResponse> constraint(ConstraintViolationException e, HttpServletRequest r) {
        return out(ErrorCode.COMMON_VALIDATION_ERROR, e.getMessage(), r);
    }

    @ExceptionHandler({
        DataIntegrityViolationException.class,
        ObjectOptimisticLockingFailureException.class
    })
    ResponseEntity<ErrorResponse> conflict(Exception e, HttpServletRequest r) {
        return out(
                ErrorCode.COMMON_CONFLICT,
                e instanceof ObjectOptimisticLockingFailureException
                        ? "The resource was modified by another request"
                        : ErrorCode.COMMON_CONFLICT.getDefaultMessage(),
                r);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> denied(AccessDeniedException e, HttpServletRequest r) {
        return out(ErrorCode.ACCESS_DENIED, ErrorCode.ACCESS_DENIED.getDefaultMessage(), r);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorResponse> typeMismatch(
            MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        return out(ErrorCode.INVALID_REQUEST, "Invalid value for " + exception.getName(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> unreadable(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        return out(ErrorCode.INVALID_REQUEST, readableMessage(exception), request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ErrorResponse> unsupportedMediaType(
            HttpMediaTypeNotSupportedException exception, HttpServletRequest request) {
        String contentType =
                exception.getContentType() == null
                        ? "unknown"
                        : exception.getContentType().toString();
        return out(
                ErrorCode.FILE_TYPE_NOT_ALLOWED,
                "Unsupported content type '"
                        + contentType
                        + "'. The metadata part must use application/json",
                request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception e, HttpServletRequest r) {
        log.error("Unexpected error {} {}", r.getMethod(), r.getRequestURI(), e);
        return out(
                ErrorCode.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage(),
                r);
    }

    private ResponseEntity<ErrorResponse> out(ErrorCode c, String m, HttpServletRequest r) {
        return ResponseEntity.status(c.getHttpStatus())
                .body(ErrorResponse.of(c, m, r.getRequestURI()));
    }

    private String readableMessage(HttpMessageNotReadableException exception) {
        Throwable cause = exception.getMostSpecificCause();
        if (cause instanceof InvalidFormatException invalidFormatException) {
            String field = fieldPath(invalidFormatException);
            Class<?> targetType = invalidFormatException.getTargetType();
            if (targetType != null && targetType.isEnum()) {
                String allowedValues =
                        Arrays.stream(targetType.getEnumConstants())
                                .map(Object::toString)
                                .collect(Collectors.joining(", "));
                return field + ": invalid value. Allowed values: " + allowedValues;
            }
            return field + ": invalid value";
        }
        if (cause instanceof JsonParseException) {
            return "Malformed JSON request body";
        }
        if (cause instanceof MismatchedInputException mismatchedInputException) {
            String field = fieldPath(mismatchedInputException);
            return field.equals("requestBody")
                    ? "Request body is required or has an invalid structure"
                    : field + ": invalid value or structure";
        }
        return ErrorCode.INVALID_REQUEST.getDefaultMessage();
    }

    private String fieldPath(MismatchedInputException exception) {
        String path =
                exception.getPath().stream()
                        .map(Reference::getFieldName)
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining("."));
        return path.isBlank() ? "requestBody" : path;
    }
}
