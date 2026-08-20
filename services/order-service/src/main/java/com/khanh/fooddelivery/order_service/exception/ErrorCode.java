package com.khanh.fooddelivery.order_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    INVALID_REQUEST("CHECKOUT_001", HttpStatus.BAD_REQUEST, "Invalid request"),
    UNAUTHENTICATED("CHECKOUT_002", HttpStatus.UNAUTHORIZED, "Authentication is required"),
    ACCESS_DENIED("CHECKOUT_003", HttpStatus.FORBIDDEN, "Access denied"),
    CART_EMPTY("CHECKOUT_004", HttpStatus.CONFLICT, "Cart is empty"),
    CART_VERSION_CONFLICT("CHECKOUT_005", HttpStatus.CONFLICT, "Cart was changed by another request"),
    ADDRESS_NOT_FOUND("CHECKOUT_006", HttpStatus.NOT_FOUND, "Address not found"),
    ITEM_UNAVAILABLE("CHECKOUT_007", HttpStatus.CONFLICT, "An item is currently unavailable"),
    INVALID_OPTION_SELECTION("CHECKOUT_008", HttpStatus.BAD_REQUEST, "Invalid option selection"),
    BRANCH_NOT_ACCEPTING_ORDERS("CHECKOUT_009", HttpStatus.CONFLICT, "Restaurant branch is not accepting orders"),
    CURRENCY_MISMATCH("CHECKOUT_010", HttpStatus.CONFLICT, "Cart items use different currencies"),
    CART_SERVICE_UNAVAILABLE("CHECKOUT_011", HttpStatus.SERVICE_UNAVAILABLE, "Cart service is unavailable"),
    USER_SERVICE_UNAVAILABLE("CHECKOUT_012", HttpStatus.SERVICE_UNAVAILABLE, "User service is unavailable"),
    CATALOG_SERVICE_UNAVAILABLE("CHECKOUT_013", HttpStatus.SERVICE_UNAVAILABLE, "Catalog service is unavailable"),
    RESTAURANT_SERVICE_UNAVAILABLE("CHECKOUT_014", HttpStatus.SERVICE_UNAVAILABLE, "Restaurant service is unavailable"),
    INTERNAL_SERVER_ERROR("CHECKOUT_015", HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred"),
    ADDRESS_COORDINATES_MISSING("CHECKOUT_016", HttpStatus.UNPROCESSABLE_ENTITY, "Delivery address coordinates are required"),
    DELIVERY_NOT_SERVICEABLE("CHECKOUT_017", HttpStatus.CONFLICT, "Delivery is unavailable for this address"),
    DELIVERY_PROVIDER_UNAVAILABLE("CHECKOUT_018", HttpStatus.SERVICE_UNAVAILABLE, "Delivery quote is temporarily unavailable"),
    ORDER_NOT_FOUND("ORDER_001", HttpStatus.NOT_FOUND, "Order not found"),
    ORDER_TRANSITION_INVALID("ORDER_002", HttpStatus.CONFLICT, "Order status transition is invalid"),
    ORDER_ACCESS_DENIED("ORDER_003", HttpStatus.FORBIDDEN, "Order access denied"),
    DELIVERY_SERVICE_UNAVAILABLE("ORDER_004", HttpStatus.SERVICE_UNAVAILABLE, "Delivery service is unavailable"),
    ORDER_NOT_PLACEABLE("ORDER_005", HttpStatus.CONFLICT, "Order cannot be placed yet");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }
}
