package com.khanh.fooddelivery.restaurant_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    RESTAURANT_APPLICATION_NOT_FOUND(
            "RAPP_001", HttpStatus.NOT_FOUND, "Restaurant application not found"),
    RESTAURANT_APPLICATION_ACCESS_DENIED(
            "RAPP_002", HttpStatus.FORBIDDEN, "Restaurant application access denied"),
    INVALID_APPLICATION_STATUS_TRANSITION(
            "RAPP_003", HttpStatus.CONFLICT, "Invalid application status transition"),
    APPLICATION_REQUIRED_DOCUMENT_MISSING(
            "RAPP_004", HttpStatus.BAD_REQUEST, "Required application document is missing"),
    APPLICATION_DOCUMENT_NOT_VERIFIED(
            "RAPP_005", HttpStatus.BAD_REQUEST, "Required application document is not verified"),
    APPLICATION_ALREADY_APPROVED(
            "RAPP_006", HttpStatus.CONFLICT, "Application already has a restaurant"),
    APPLICATION_DOCUMENT_NOT_FOUND(
            "RDOC_001", HttpStatus.NOT_FOUND, "Application document not found"),
    RESTAURANT_NOT_FOUND("REST_001", HttpStatus.NOT_FOUND, "Restaurant not found"),
    RESTAURANT_ACCESS_DENIED("REST_002", HttpStatus.FORBIDDEN, "Restaurant access denied"),
    INVALID_RESTAURANT_STATUS_TRANSITION(
            "REST_003", HttpStatus.CONFLICT, "Invalid restaurant status transition"),
    BRANCH_NOT_FOUND("BRANCH_001", HttpStatus.NOT_FOUND, "Restaurant branch not found"),
    BRANCH_CODE_ALREADY_EXISTS("BRANCH_002", HttpStatus.CONFLICT, "Branch code already exists"),
    BRANCH_NOT_ACTIVE("BRANCH_003", HttpStatus.CONFLICT, "Branch is not active"),
    INVALID_BUSINESS_HOURS("BRANCH_004", HttpStatus.BAD_REQUEST, "Invalid business hours"),
    RESTAURANT_MEMBER_NOT_FOUND("MEMBER_001", HttpStatus.NOT_FOUND, "Restaurant member not found"),
    RESTAURANT_MEMBER_ALREADY_EXISTS(
            "MEMBER_002", HttpStatus.CONFLICT, "Restaurant member already exists in this scope"),
    OWNER_MEMBER_CANNOT_BE_REMOVED(
            "MEMBER_003", HttpStatus.CONFLICT, "Owner member cannot be removed"),
    INVALID_MEMBER_SCOPE("MEMBER_004", HttpStatus.BAD_REQUEST, "Invalid restaurant member scope"),
    BANK_ACCOUNT_NOT_FOUND("BANK_001", HttpStatus.NOT_FOUND, "Bank account not found"),
    BANK_ACCOUNT_ALREADY_EXISTS("BANK_002", HttpStatus.CONFLICT, "Bank account already exists"),
    BANK_ACCOUNT_NOT_VERIFIED("BANK_003", HttpStatus.CONFLICT, "Bank account is not verified"),
    FILE_EMPTY("FILE_001", HttpStatus.BAD_REQUEST, "File must not be empty"),
    FILE_TOO_LARGE(
            "FILE_002", HttpStatus.PAYLOAD_TOO_LARGE, "File exceeds the configured size limit"),
    FILE_TYPE_NOT_ALLOWED(
            "FILE_003", HttpStatus.UNSUPPORTED_MEDIA_TYPE, "File content type is not allowed"),
    FILE_UPLOAD_FAILED("FILE_004", HttpStatus.INTERNAL_SERVER_ERROR, "File upload failed"),
    FILE_DELETE_FAILED("FILE_005", HttpStatus.INTERNAL_SERVER_ERROR, "File deletion failed"),
    FILE_STORAGE_NOT_CONFIGURED(
            "FILE_006", HttpStatus.INTERNAL_SERVER_ERROR, "File storage is not configured"),
    COMMON_VALIDATION_ERROR("COMMON_002", HttpStatus.BAD_REQUEST, "Validation failed"),
    COMMON_CONFLICT("COMMON_005", HttpStatus.CONFLICT, "Data conflict"),
    ACCESS_DENIED("COMMON_003", HttpStatus.FORBIDDEN, "Access denied"),
    UNAUTHENTICATED("COMMON_004", HttpStatus.UNAUTHORIZED, "Authentication is required"),
    INVALID_REQUEST("COMMON_001", HttpStatus.BAD_REQUEST, "Invalid request"),
    INTERNAL_SERVER_ERROR(
            "COMMON_006", HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.httpStatus = status;
        this.defaultMessage = message;
    }
}
