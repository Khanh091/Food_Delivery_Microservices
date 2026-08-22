package com.khanh.fooddelivery.notification_service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khanh.fooddelivery.notification_service.service.DriverOfferNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryOfferCreatedConsumer {

    private final ObjectMapper objectMapper;
    private final DriverOfferNotificationService notifications;

    @KafkaListener(
            topics = "${app.kafka.delivery-lifecycle-topic:delivery.lifecycle.v1}",
            groupId = "${app.kafka.delivery-lifecycle-consumer-group:notification-service-delivery-lifecycle}",
            containerFactory = "deliveryLifecycleKafkaListenerContainerFactory"
    )
    public void consume(String message) {
        DeliveryLifecycleEvent event;
        try {
            event = objectMapper.readValue(message, DeliveryLifecycleEvent.class);
            validate(event);
        } catch (Exception exception) {
            log.error("Invalid delivery lifecycle event reasonType={}", exception.getClass().getSimpleName());
            throw new IllegalArgumentException("Invalid delivery lifecycle event", exception);
        }

        log.info("Delivery lifecycle event received eventId={} eventType={} offerId={}",
                event.eventId(), event.eventType(), event.payload().offerId());
        notifications.notifyDriverOffer(event);
    }

    private void validate(DeliveryLifecycleEvent event) {
        if (event == null
                || event.eventId() == null
                || event.occurredAt() == null
                || event.aggregateId() == null
                || event.version() != DeliveryLifecycleEvent.VERSION
                || !DeliveryLifecycleEvent.DELIVERY_OFFER_CREATED.equals(event.eventType())
                || event.payload() == null
                || event.payload().offerId() == null
                || event.payload().deliveryId() == null
                || event.payload().driverId() == null
                || event.payload().expiresAt() == null
                || !event.aggregateId().equals(event.payload().offerId())) {
            throw new IllegalArgumentException("Invalid DELIVERY_OFFER_CREATED event");
        }
    }
}
