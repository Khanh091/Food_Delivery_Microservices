package com.khanh.fooddelivery.delivery_service.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.khanh.fooddelivery.delivery_service.event.DeliveryOfferCreatedEvent;
import com.khanh.fooddelivery.delivery_service.model.DeliveryOffer;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeliveryOutboxServiceImplTests {

    @Mock
    private DeliveryOutboxEventRepository repository;

    private DeliveryOutboxServiceImpl service;
    private DeliveryOffer offer;

    @BeforeEach
    void setUp() {
        service = new DeliveryOutboxServiceImpl(
                repository,
                new ObjectMapper().registerModule(new JavaTimeModule()),
                Clock.fixed(Instant.parse("2026-08-23T10:15:30Z"), ZoneOffset.UTC)
        );
        offer = new DeliveryOffer();
        offer.setId(UUID.randomUUID());
        offer.setDeliveryId(UUID.randomUUID());
        offer.setDriverId(UUID.randomUUID());
        offer.setExpiresAt(Instant.parse("2026-08-23T10:16:15Z"));
    }

    @Test
    void createsOfferCreatedEnvelopeWithOnlyNotificationData() {
        when(repository.existsByAggregateTypeAndAggregateIdAndEventType(
                "DELIVERY_OFFER",
                offer.getId(),
                DeliveryOfferCreatedEvent.EVENT_TYPE
        )).thenReturn(false);

        service.publishDeliveryOfferCreated(offer);

        ArgumentCaptor<DeliveryOutboxEvent> captor = ArgumentCaptor.forClass(DeliveryOutboxEvent.class);
        verify(repository).save(captor.capture());
        DeliveryOutboxEvent outbox = captor.getValue();
        assertThat(outbox.getAggregateId()).isEqualTo(offer.getId());
        assertThat(outbox.getEventType()).isEqualTo(DeliveryOfferCreatedEvent.EVENT_TYPE);
        assertThat(outbox.getPayload().get("eventId").asText()).isNotBlank();
        assertThat(outbox.getPayload().get("payload").get("offerId").asText())
                .isEqualTo(offer.getId().toString());
        assertThat(outbox.getPayload().get("payload").get("driverId").asText())
                .isEqualTo(offer.getDriverId().toString());
        assertThat(Instant.ofEpochSecond(outbox.getPayload().get("payload").get("expiresAt").asLong()))
                .isEqualTo(offer.getExpiresAt());
        assertThat(outbox.getPayload().get("payload").get("token")).isNull();
    }

    @Test
    void duplicateSemanticOfferEventDoesNotCreateAnotherRow() {
        when(repository.existsByAggregateTypeAndAggregateIdAndEventType(
                "DELIVERY_OFFER",
                offer.getId(),
                DeliveryOfferCreatedEvent.EVENT_TYPE
        )).thenReturn(true);

        service.publishDeliveryOfferCreated(offer);

        verify(repository, never()).save(any(DeliveryOutboxEvent.class));
    }
}
