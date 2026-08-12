package com.khanh.fooddelivery.restaurant_service.outbox;

public enum RestaurantEventType {
    RESTAURANT_UPSERTED,
    RESTAURANT_STATUS_CHANGED,
    RESTAURANT_BRANCH_UPSERTED,
    RESTAURANT_BRANCH_STATUS_CHANGED
}
