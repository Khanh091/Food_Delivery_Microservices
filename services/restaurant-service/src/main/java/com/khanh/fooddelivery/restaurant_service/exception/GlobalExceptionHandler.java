package com.khanh.fooddelivery.restaurant_service.exception;
import jakarta.servlet.http.HttpServletRequest; import jakarta.validation.ConstraintViolationException; import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException; import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException; import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException; import org.springframework.validation.FieldError; import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*; import java.util.Comparator; import java.util.stream.Collectors;
@Slf4j @RestControllerAdvice public class GlobalExceptionHandler {
 @ExceptionHandler(AppException.class) ResponseEntity<ErrorResponse> app(AppException e,HttpServletRequest r){return out(e.getErrorCode(),e.getMessage(),r);}
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException e,HttpServletRequest r){String m=e.getBindingResult().getFieldErrors().stream().sorted(Comparator.comparing(FieldError::getField)).map(x->x.getField()+": "+x.getDefaultMessage()).collect(Collectors.joining("; "));return out(ErrorCode.COMMON_VALIDATION_ERROR,m,r);}
 @ExceptionHandler(ConstraintViolationException.class) ResponseEntity<ErrorResponse> constraint(ConstraintViolationException e,HttpServletRequest r){return out(ErrorCode.COMMON_VALIDATION_ERROR,e.getMessage(),r);}
 @ExceptionHandler({DataIntegrityViolationException.class,ObjectOptimisticLockingFailureException.class}) ResponseEntity<ErrorResponse> conflict(Exception e,HttpServletRequest r){return out(ErrorCode.COMMON_CONFLICT,e instanceof ObjectOptimisticLockingFailureException?"The resource was modified by another request":ErrorCode.COMMON_CONFLICT.getDefaultMessage(),r);}
 @ExceptionHandler(AccessDeniedException.class) ResponseEntity<ErrorResponse> denied(AccessDeniedException e,HttpServletRequest r){return out(ErrorCode.ACCESS_DENIED,ErrorCode.ACCESS_DENIED.getDefaultMessage(),r);}
 @ExceptionHandler(HttpMessageNotReadableException.class) ResponseEntity<ErrorResponse> unreadable(Exception e,HttpServletRequest r){return out(ErrorCode.INVALID_REQUEST,ErrorCode.INVALID_REQUEST.getDefaultMessage(),r);}
 @ExceptionHandler(Exception.class) ResponseEntity<ErrorResponse> unexpected(Exception e,HttpServletRequest r){log.error("Unexpected error {} {}",r.getMethod(),r.getRequestURI(),e);return out(ErrorCode.INTERNAL_SERVER_ERROR,ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage(),r);}
 private ResponseEntity<ErrorResponse> out(ErrorCode c,String m,HttpServletRequest r){return ResponseEntity.status(c.getHttpStatus()).body(ErrorResponse.of(c,m,r.getRequestURI()));}
}
