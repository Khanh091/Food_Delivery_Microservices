package com.khanh.fooddelivery.notification_service.event;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.khanh.fooddelivery.notification_service.service.DriverOfferNotificationService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class DeliveryOfferCreatedConsumerTests {

    @Mock
    private DriverOfferNotificationService notifications;

    private DeliveryOfferCreatedConsumer consumer;
    private ObjectMapper objectMapper;
    private DeliveryOfferCreatedEvent event;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        consumer = new DeliveryOfferCreatedConsumer(objectMapper, notifications);
        UUID offerId = UUID.randomUUID();
        event = new DeliveryOfferCreatedEvent(
                UUID.randomUUID(),
                DeliveryOfferCreatedEvent.EVENT_TYPE,
                Instant.parse("2026-08-23T10:15:30Z"),
                offerId,
                DeliveryOfferCreatedEvent.VERSION,
                new DeliveryOfferCreatedEvent.Payload(
                        offerId,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Instant.parse("2026-08-23T10:16:15Z")
                )
        );
    }

    @Test
    void validEventDelegatesWithoutSynchronousDeliveryCall() throws Exception {
        consumer.consume(objectMapper.writeValueAsString(event));

        verify(notifications).notifyDriverOffer(event);
    }

    @Test
    void malformedEventIsRejectedForDltHandling() {
        assertThatThrownBy(() -> consumer.consume("{\"eventType\":\"DELIVERY_OFFER_CREATED\"}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid delivery lifecycle event");
    }

    @Test
    void impossibleAggregateIdentityIsRejected() throws Exception {
        DeliveryOfferCreatedEvent invalid = new DeliveryOfferCreatedEvent(
                event.eventId(),
                event.eventType(),
                event.occurredAt(),
                UUID.randomUUID(),
                event.version(),
                event.payload()
        );

        assertThatThrownBy(() -> consumer.consume(objectMapper.writeValueAsString(invalid)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
