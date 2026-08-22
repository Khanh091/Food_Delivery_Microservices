package com.khanh.fooddelivery.delivery_service.service;

import com.khanh.fooddelivery.delivery_service.event.OrderConfirmedEvent;
import com.khanh.fooddelivery.delivery_service.model.Delivery;

public interface DeliveryCreationService {

    Delivery ensureMatching(OrderConfirmedEvent.Payload payload);
}
