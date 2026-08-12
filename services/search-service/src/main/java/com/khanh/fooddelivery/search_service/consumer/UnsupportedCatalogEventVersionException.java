package com.khanh.fooddelivery.search_service.consumer;

import com.khanh.fooddelivery.search_service.event.DomainEventEnvelope;

public class UnsupportedCatalogEventVersionException extends RuntimeException {
    public UnsupportedCatalogEventVersionException(DomainEventEnvelope event) {
        super(
                "Unsupported catalog event version eventId=%s eventType=%s version=%d"
                        .formatted(event.eventId(), event.eventType(), event.eventVersion()));
    }
}
