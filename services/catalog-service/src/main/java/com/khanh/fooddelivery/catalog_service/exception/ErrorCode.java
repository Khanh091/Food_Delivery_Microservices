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
            "CATALOG_015", HttpStatus.NOT_FOUND, "Menu category item not found"),
    BRANCH_ITEM_NOT_FOUND("CATALOG_016", HttpStatus.NOT_FOUND, "Branch item not found"),
    BRANCH_ITEM_ALREADY_EXISTS("CATALOG_017", HttpStatus.CONFLICT, "Branch item already exists"),
    INVALID_SOLD_OUT_TIME(
            "CATALOG_018", HttpStatus.BAD_REQUEST, "Sold out time must be in the future"),
    OPTION_GROUP_NOT_FOUND("CATALOG_019", HttpStatus.NOT_FOUND, "Option group not found"),
    OPTION_VALUE_NOT_FOUND("CATALOG_020", HttpStatus.NOT_FOUND, "Option value not found"),
    INVALID_OPTION_SELECTION(
            "CATALOG_021", HttpStatus.BAD_REQUEST, "Invalid option selection rules");
    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }
}
