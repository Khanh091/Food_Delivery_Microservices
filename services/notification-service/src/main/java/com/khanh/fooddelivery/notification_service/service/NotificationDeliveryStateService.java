package com.khanh.fooddelivery.notification_service.service;

import com.khanh.fooddelivery.notification_service.entity.NotificationDelivery;
import com.khanh.fooddelivery.notification_service.event.DeliveryLifecycleEvent;
import java.util.UUID;

public interface NotificationDeliveryStateService {

    NotificationDelivery prepare(DeliveryLifecycleEvent event);

    void markSent(UUID notificationId);

    void markFailed(UUID notificationId, Throwable failure);

    void markSkipped(UUID notificationId, String reason);
}
