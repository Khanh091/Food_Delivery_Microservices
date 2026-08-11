package com.khanh.fooddelivery.catalog_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    INVALID_REQUEST("CATALOG_001", HttpStatus.BAD_REQUEST, "Invalid request"),
    ACCESS_DENIED("CATALOG_002", HttpStatus.FORBIDDEN, "Access denied"),
    UNAUTHENTICATED("CATALOG_003", HttpStatus.UNAUTHORIZED, "Authentication is required"),
    DATA_CONFLICT("CATALOG_004", HttpStatus.CONFLICT, "Data conflict"),
    INTERNAL_SERVER_ERROR(
            "CATALOG_005", HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred"),
    RESTAURANT_NOT_FOUND("CATALOG_006", HttpStatus.NOT_FOUND, "Restaurant not found"),
    BRANCH_NOT_FOUND("CATALOG_007", HttpStatus.NOT_FOUND, "Restaurant branch not found"),
    RESTAURANT_SERVICE_UNAVAILABLE(
            "CATALOG_008", HttpStatus.SERVICE_UNAVAILABLE, "Restaurant service is unavailable"),
    MENU_NOT_FOUND("CATALOG_009", HttpStatus.NOT_FOUND, "Menu not found"),
    MENU_CATEGORY_NOT_FOUND("CATALOG_010", HttpStatus.NOT_FOUND, "Menu category not found"),
    INVALID_MENU_DATE_RANGE(
            "CATALOG_011", HttpStatus.BAD_REQUEST, "Invalid menu availability date range"),
    CATALOG_ITEM_NOT_FOUND("CATALOG_012", HttpStatus.NOT_FOUND, "Catalog item not found"),
    ITEM_CATEGORY_MISMATCH(
            "CATALOG_013",
            HttpStatus.BAD_REQUEST,
            "Catalog item does not belong to the category restaurant"),
    ITEM_ALREADY_IN_CATEGORY(
            "CATALOG_014", HttpStatus.CONFLICT, "Catalog item is already in the category"),
    MENU_CATEGORY_ITEM_NOT_FOUND(
            "CATALOG_015", HttpStatus.NOT_FOUND, "Menu category item not found");
    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }
}
