package com.khanh.fooddelivery.catalog_service.outbox;

import java.util.UUID;

public interface OutboxEventService {
    void enqueue(CatalogEventType eventType, String aggregateType, UUID aggregateId, Object data);
}
