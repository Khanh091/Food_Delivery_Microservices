package com.khanh.fooddelivery.search_service.exception;

import org.springframework.http.HttpStatus;

public enum SearchErrorCode {
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "SEARCH_001", "Authentication is required"),
    REBUILD_IN_PROGRESS(HttpStatus.CONFLICT, "SEARCH_002", "Catalog search rebuild is already in progress"),
    INDEX_RECREATE_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "SEARCH_003", "Unable to recreate search index"),
    CATALOG_REINDEX_TRIGGER_FAILED(
            HttpStatus.BAD_GATEWAY, "SEARCH_004", "Unable to trigger catalog snapshot reindex"),
    RESTAURANT_REINDEX_TRIGGER_FAILED(
            HttpStatus.BAD_GATEWAY, "SEARCH_005", "Unable to trigger restaurant snapshot reindex"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "SEARCH_006", "Invalid request");

    private final HttpStatus status;
    private final String code;
    private final String message;

    SearchErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
