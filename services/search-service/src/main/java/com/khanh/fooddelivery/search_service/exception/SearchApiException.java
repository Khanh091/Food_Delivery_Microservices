package com.khanh.fooddelivery.search_service.exception;

public class SearchApiException extends RuntimeException {
    private final SearchErrorCode errorCode;

    public SearchApiException(SearchErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public SearchApiException(SearchErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public SearchErrorCode getErrorCode() {
        return errorCode;
    }
}
