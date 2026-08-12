package com.khanh.fooddelivery.search_service.consumer;

public class UnsupportedRestaurantEventVersionException extends RuntimeException {
    public UnsupportedRestaurantEventVersionException(int version) { super("Unsupported restaurant event version: " + version); }
}
