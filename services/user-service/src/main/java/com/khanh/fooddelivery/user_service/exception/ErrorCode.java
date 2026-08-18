package com.khanh.fooddelivery.user_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    USER_NOT_FOUND("USR_001", HttpStatus.NOT_FOUND, "User not found"),
    EMAIL_ALREADY_EXISTS(
            "USR_002",
            HttpStatus.CONFLICT,
            "Email already exists"
    ),
    PHONE_NUMBER_ALREADY_EXISTS(
            "USR_003",
            HttpStatus.CONFLICT,
            "Phone number already exists"
    ),
    AVATAR_FILE_INVALID("USR_004", HttpStatus.BAD_REQUEST, "Avatar file must be an image"),
    AVATAR_FILE_TOO_LARGE("USR_005", HttpStatus.PAYLOAD_TOO_LARGE, "Avatar file is too large"),
    AVATAR_STORAGE_NOT_CONFIGURED("USR_006", HttpStatus.SERVICE_UNAVAILABLE, "Avatar storage is not configured"),
    AVATAR_UPLOAD_FAILED("USR_007", HttpStatus.BAD_GATEWAY, "Avatar upload failed"),
    AVATAR_DELETE_FAILED("USR_008", HttpStatus.BAD_GATEWAY, "Avatar deletion failed"),
    SYSTEM_ROLE_PROVISIONING_FAILED(
            "USR_009", HttpStatus.BAD_GATEWAY, "System role provisioning is temporarily unavailable"),
    SYSTEM_ROLE_NOT_GRANTABLE(
            "USR_010", HttpStatus.FORBIDDEN, "System role is not grantable through partner flows"),
    ADDRESS_NOT_FOUND("ADDR_001", HttpStatus.NOT_FOUND, "Address not found"),
    INVALID_REQUEST(
            "COMMON_001",
            HttpStatus.BAD_REQUEST,
            "Invalid request"
    ),
    VALIDATION_ERROR(
            "COMMON_002",
            HttpStatus.BAD_REQUEST,
            "Validation failed"
    ),
    ACCESS_DENIED("COMMON_003", HttpStatus.FORBIDDEN, "Access denied"),
    UNAUTHENTICATED(
            "COMMON_004",
            HttpStatus.UNAUTHORIZED,
            "Authentication is required"
    ),
    DATA_CONFLICT("COMMON_005", HttpStatus.CONFLICT, "Data conflict"),
    INTERNAL_SERVER_ERROR(
            "COMMON_006",
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred"
    );

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(
            String code,
            HttpStatus httpStatus,
            String defaultMessage
    ) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }
}
