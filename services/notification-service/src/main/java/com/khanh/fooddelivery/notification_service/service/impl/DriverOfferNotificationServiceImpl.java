package com.khanh.fooddelivery.notification_service.service.impl;

import com.khanh.fooddelivery.notification_service.client.ExpoPushClient;
import com.khanh.fooddelivery.notification_service.entity.NotificationDelivery;
import com.khanh.fooddelivery.notification_service.entity.NotificationDeliveryStatus;
import com.khanh.fooddelivery.notification_service.entity.PushDevice;
import com.khanh.fooddelivery.notification_service.event.DeliveryOfferCreatedEvent;
import com.khanh.fooddelivery.notification_service.repository.PushDeviceRepository;
import com.khanh.fooddelivery.notification_service.service.DriverOfferNotificationService;
import com.khanh.fooddelivery.notification_service.service.NotificationDeliveryStateService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverOfferNotificationServiceImpl implements DriverOfferNotificationService {

    private final NotificationDeliveryStateService states;
    private final PushDeviceRepository devices;
    private final ExpoPushClient expo;

    @Override
    public void notifyDriverOffer(DeliveryOfferCreatedEvent event) {
        NotificationDelivery notification = states.prepare(event);
        if (notification.getStatus() == NotificationDeliveryStatus.SENT
                || notification.getStatus() == NotificationDeliveryStatus.SKIPPED) {
            log.info("Driver offer notification already resolved eventId={} offerId={} status={}",
                    event.eventId(), event.payload().offerId(), notification.getStatus());
            return;
        }

        List<PushDevice> activeDevices = devices.findByDriverIdAndActiveTrue(event.payload().driverId());
        if (activeDevices.isEmpty()) {
            states.markSkipped(notification.getId(), "No active push device");
            log.info("Driver offer notification skipped because driver has no active push device eventId={} offerId={}",
                    event.eventId(), event.payload().offerId());
            return;
        }

        boolean sent = false;
        try {
            for (PushDevice device : activeDevices) {
                ExpoPushClient.DeliveryResult result = expo.send(
                        device.getExpoPushToken(),
                        "Chuyến giao mới",
                        "Bạn có một chuyến giao mới cần xác nhận.",
                        Map.of(
                                "type", "DELIVERY_OFFER",
                                "offerId", event.payload().offerId().toString(),
                                "deliveryId", event.payload().deliveryId().toString(),
                                "expiresAt", event.payload().expiresAt().toString()
                        )
                );
                if (result == ExpoPushClient.DeliveryResult.INVALID_TOKEN) {
                    device.setActive(false);
                    devices.save(device);
                } else if (result == ExpoPushClient.DeliveryResult.TRANSIENT_FAILURE) {
                    throw new IllegalStateException("Expo push temporarily unavailable");
                } else {
                    sent = true;
                }
            }
        } catch (RuntimeException exception) {
            states.markFailed(notification.getId(), exception);
            log.warn("Driver offer notification failed eventId={} offerId={} reasonType={}",
                    event.eventId(), event.payload().offerId(), exception.getClass().getSimpleName());
            throw exception;
        }

        if (sent) {
            states.markSent(notification.getId());
            log.info("Driver offer notification sent eventId={} offerId={} driverId={}",
                    event.eventId(), event.payload().offerId(), event.payload().driverId());
        } else {
            states.markSkipped(notification.getId(), "No valid active push device");
            log.info("Driver offer notification skipped because all push devices are invalid eventId={} offerId={}",
                    event.eventId(), event.payload().offerId());
        }
    }
}
