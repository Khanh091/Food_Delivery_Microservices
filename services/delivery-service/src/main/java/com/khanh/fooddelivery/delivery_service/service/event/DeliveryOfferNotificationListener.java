package com.khanh.fooddelivery.delivery_service.service.event;

import com.khanh.fooddelivery.delivery_service.client.NotificationServiceClient;
import com.khanh.fooddelivery.delivery_service.client.dto.request.DriverOfferNotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryOfferNotificationListener {

    private final NotificationServiceClient notifications;

    @Value("${app.internal-api.key:}")
    private String internalApiKey;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notifyDriver(DeliveryOfferCreatedEvent event) {
        if (internalApiKey == null || internalApiKey.isBlank()) {
            log.debug("Skipping driver offer notification because internal API key is not configured");
            return;
        }
        try {
            notifications.notifyDriverOffer(
                    internalApiKey,
                    new DriverOfferNotificationRequest(
                            event.driverId(),
                            event.offerId(),
                            event.deliveryId()
                    )
            );
        } catch (Exception exception) {
            // Push is a wake-up signal. The persisted offer remains authoritative.
            log.warn(
                    "Driver offer notification failed for driver {} and delivery {}: {}",
                    event.driverId(),
                    event.deliveryId(),
                    exception.getClass().getSimpleName()
            );
        }
    }
}
