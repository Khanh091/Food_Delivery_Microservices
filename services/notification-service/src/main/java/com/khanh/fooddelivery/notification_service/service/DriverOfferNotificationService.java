package com.khanh.fooddelivery.notification_service.service;

import com.khanh.fooddelivery.notification_service.event.DeliveryLifecycleEvent;

public interface DriverOfferNotificationService {

    void notifyDriverOffer(DeliveryLifecycleEvent event);
}
