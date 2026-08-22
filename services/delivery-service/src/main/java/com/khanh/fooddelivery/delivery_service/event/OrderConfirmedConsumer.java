package com.khanh.fooddelivery.delivery_service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khanh.fooddelivery.delivery_service.service.DeliveryCreationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderConfirmedConsumer {

    private final ObjectMapper objectMapper;
    private final DeliveryCreationService deliveryCreation;

    @KafkaListener(
            topics = "${app.kafka.order-confirmed-topic:order.lifecycle.v1}",
            groupId = "${app.kafka.consumer-group:delivery-service-order-lifecycle}"
    )
    public void consume(String message) {
        try {
            OrderConfirmedEvent event = objectMapper.readValue(message, OrderConfirmedEvent.class);
            if (event.eventId() == null
                    || event.version() != OrderConfirmedEvent.VERSION
                    || event.occurredAt() == null
                    || !OrderConfirmedEvent.TYPE.equals(event.eventType())
                    || event.payload() == null
                    || event.aggregateId() == null
                    || event.payload().orderId() == null
                    || !event.aggregateId().equals(event.payload().orderId())) {
                throw new IllegalArgumentException("Invalid ORDER_CONFIRMED event");
            }
            deliveryCreation.ensureMatching(event.payload());
            log.info("OrderConfirmed event handled eventId={} orderId={}",
                    event.eventId(), event.aggregateId());
        } catch (Exception exception) {
            log.error("OrderConfirmed event processing failed", exception);
            throw new IllegalStateException("OrderConfirmed event processing failed", exception);
        }
    }
}
