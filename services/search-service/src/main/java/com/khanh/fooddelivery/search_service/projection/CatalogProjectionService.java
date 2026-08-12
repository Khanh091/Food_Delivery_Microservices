package com.khanh.fooddelivery.search_service.projection;

import com.khanh.fooddelivery.search_service.event.DomainEventEnvelope;

public interface CatalogProjectionService {
    void apply(DomainEventEnvelope event);
}
