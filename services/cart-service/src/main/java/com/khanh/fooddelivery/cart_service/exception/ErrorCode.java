package com.khanh.fooddelivery.cart_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    INVALID_REQUEST("CART_001", HttpStatus.BAD_REQUEST, "Invalid request"),
    QUANTITY_OUT_OF_RANGE("CART_002", HttpStatus.BAD_REQUEST, "Quantity must be between 1 and 99"),
    INVALID_OPTION_SELECTION("CART_003", HttpStatus.BAD_REQUEST, "Invalid option selection"),
    UNAUTHENTICATED("CART_004", HttpStatus.UNAUTHORIZED, "Authentication is required"),
    ACCESS_DENIED("CART_005", HttpStatus.FORBIDDEN, "Access denied"),
    CATALOG_ITEM_NOT_FOUND("CART_006", HttpStatus.NOT_FOUND, "Catalog item not found"),
    BRANCH_ITEM_NOT_FOUND("CART_007", HttpStatus.NOT_FOUND, "Branch item not found"),
    CART_ITEM_NOT_FOUND("CART_008", HttpStatus.NOT_FOUND, "Cart item not found"),
    ITEM_UNAVAILABLE("CART_009", HttpStatus.CONFLICT, "Item is currently unavailable"),
    BRANCH_NOT_ACCEPTING_ORDERS(
            "CART_010", HttpStatus.CONFLICT, "Restaurant branch is not accepting orders"),
    CART_VERSION_CONFLICT("CART_012", HttpStatus.CONFLICT, "Cart was changed by another request"),
    CATALOG_SERVICE_UNAVAILABLE(
            "CART_013", HttpStatus.SERVICE_UNAVAILABLE, "Catalog service is unavailable"),
    RESTAURANT_SERVICE_UNAVAILABLE(
            "CART_014", HttpStatus.SERVICE_UNAVAILABLE, "Restaurant service is unavailable"),
    USER_SERVICE_UNAVAILABLE(
            "CART_015", HttpStatus.SERVICE_UNAVAILABLE, "User service is unavailable"),
    INTERNAL_SERVER_ERROR(
            "CART_016", HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }
}
