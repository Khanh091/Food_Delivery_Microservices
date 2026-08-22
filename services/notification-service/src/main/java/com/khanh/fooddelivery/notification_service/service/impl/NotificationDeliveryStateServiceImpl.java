package com.khanh.fooddelivery.notification_service.service.impl;

import com.khanh.fooddelivery.notification_service.entity.NotificationDelivery;
import com.khanh.fooddelivery.notification_service.entity.NotificationDeliveryStatus;
import com.khanh.fooddelivery.notification_service.event.DeliveryLifecycleEvent;
import com.khanh.fooddelivery.notification_service.repository.NotificationDeliveryRepository;
import com.khanh.fooddelivery.notification_service.service.NotificationDeliveryStateService;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationDeliveryStateServiceImpl implements NotificationDeliveryStateService {

    private final NotificationDeliveryRepository notifications;
    private final Clock clock;

    @Override
    @Transactional
    public NotificationDelivery prepare(DeliveryLifecycleEvent event) {
        DeliveryLifecycleEvent.Payload payload = event.payload();
        NotificationDelivery notification = notifications.findBySourceEventId(event.eventId())
                .orElseGet(() -> create(event));
        validateIdentity(notification, event);

        if (notification.getStatus() == NotificationDeliveryStatus.SENT
                || notification.getStatus() == NotificationDeliveryStatus.SKIPPED) {
            return notification;
        }

        if (!payload.expiresAt().isAfter(clock.instant())) {
            notification.setStatus(NotificationDeliveryStatus.SKIPPED);
            notification.setLastError(null);
            return notification;
        }

        notification.setStatus(NotificationDeliveryStatus.PENDING);
        notification.setAttemptCount(notification.getAttemptCount() + 1);
        notification.setLastError(null);
        return notification;
    }

    @Override
    @Transactional
    public void markSent(UUID notificationId) {
        notifications.findById(notificationId).ifPresent(notification -> {
            if (notification.getStatus() != NotificationDeliveryStatus.SKIPPED) {
                notification.setStatus(NotificationDeliveryStatus.SENT);
                notification.setLastError(null);
            }
        });
    }

    @Override
    @Transactional
    public void markFailed(UUID notificationId, Throwable failure) {
        notifications.findById(notificationId).ifPresent(notification -> {
            if (notification.getStatus() != NotificationDeliveryStatus.SENT
                    && notification.getStatus() != NotificationDeliveryStatus.SKIPPED) {
                notification.setStatus(NotificationDeliveryStatus.FAILED);
                String message = failure.getClass().getSimpleName() + ": " + String.valueOf(failure.getMessage());
                notification.setLastError(message.length() > 1000 ? message.substring(0, 1000) : message);
            }
        });
    }

    @Override
    @Transactional
    public void markSkipped(UUID notificationId, String reason) {
        notifications.findById(notificationId).ifPresent(notification -> {
            if (notification.getStatus() != NotificationDeliveryStatus.SENT) {
                notification.setStatus(NotificationDeliveryStatus.SKIPPED);
                notification.setLastError(reason);
            }
        });
    }

    private NotificationDelivery create(DeliveryLifecycleEvent event) {
        DeliveryLifecycleEvent.Payload payload = event.payload();
        NotificationDelivery notification = new NotificationDelivery();
        notification.setId(UUID.randomUUID());
        notification.setSourceEventId(event.eventId());
        notification.setOfferId(payload.offerId());
        notification.setDeliveryId(payload.deliveryId());
        notification.setDriverId(payload.driverId());
        notification.setExpiresAt(payload.expiresAt());
        notification.setStatus(NotificationDeliveryStatus.PENDING);
        notification.setAttemptCount(0);
        return notifications.save(notification);
    }

    private void validateIdentity(NotificationDelivery notification, DeliveryLifecycleEvent event) {
        DeliveryLifecycleEvent.Payload payload = event.payload();
        if (!Objects.equals(notification.getOfferId(), payload.offerId())
                || !Objects.equals(notification.getDeliveryId(), payload.deliveryId())
                || !Objects.equals(notification.getDriverId(), payload.driverId())
                || !Objects.equals(notification.getExpiresAt(), payload.expiresAt())) {
            throw new IllegalArgumentException("Notification event identity changed for source event");
        }
    }
}
