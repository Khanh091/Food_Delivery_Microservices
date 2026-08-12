package com.khanh.fooddelivery.search_service.consumer;

public class InvalidRestaurantEventEnvelopeException extends RuntimeException {
    public InvalidRestaurantEventEnvelopeException(String message) { super(message); }
    public InvalidRestaurantEventEnvelopeException(String message, Throwable cause) { super(message, cause); }
}
