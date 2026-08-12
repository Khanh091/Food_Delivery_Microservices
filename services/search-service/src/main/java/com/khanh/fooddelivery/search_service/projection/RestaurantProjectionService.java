package com.khanh.fooddelivery.search_service.projection;

import com.khanh.fooddelivery.search_service.event.RestaurantDomainEventEnvelope;
public interface RestaurantProjectionService { void apply(RestaurantDomainEventEnvelope event); }
