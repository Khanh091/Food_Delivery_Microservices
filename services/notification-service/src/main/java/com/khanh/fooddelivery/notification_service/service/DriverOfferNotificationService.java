package com.khanh.fooddelivery.notification_service.service;

import com.khanh.fooddelivery.notification_service.event.DeliveryOfferCreatedEvent;

public interface DriverOfferNotificationService {

    void notifyDriverOffer(DeliveryOfferCreatedEvent event);
}
