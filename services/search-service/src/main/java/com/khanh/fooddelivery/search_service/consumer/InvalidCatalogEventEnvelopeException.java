package com.khanh.fooddelivery.search_service.consumer;

public class InvalidCatalogEventEnvelopeException extends RuntimeException {
    public InvalidCatalogEventEnvelopeException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidCatalogEventEnvelopeException(String message) {
        super(message);
    }
}
