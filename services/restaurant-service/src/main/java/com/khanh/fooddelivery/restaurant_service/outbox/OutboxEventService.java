package com.khanh.fooddelivery.restaurant_service.outbox;

import java.util.UUID;

public interface OutboxEventService {
    void enqueue(RestaurantEventType type, String aggregateType, UUID aggregateId, Object data);
}
