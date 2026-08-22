package com.khanh.fooddelivery.delivery_service.service;

import com.khanh.fooddelivery.delivery_service.model.Delivery;
import java.time.Instant;
import java.util.UUID;

public interface DriverDispatchService {

    void dispatch(UUID deliveryId);

    void schedule(Delivery delivery, Instant nextDispatchAt);
}
