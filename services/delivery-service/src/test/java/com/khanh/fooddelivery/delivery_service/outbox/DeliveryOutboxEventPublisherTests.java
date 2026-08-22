package com.khanh.fooddelivery.delivery_service.outbox;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class DeliveryOutboxEventPublisherTests {

    @Mock
    private DeliveryOutboxEventClaimService claims;
    @Mock
    private DeliveryOutboxEventStateService states;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private DeliveryOutboxProperties properties;
    private DeliveryOutboxEvent event;
    private DeliveryOutboxEventPublisher publisher;

    @BeforeEach
    void setUp() {
        properties = new DeliveryOutboxProperties();
        properties.setTopic("delivery.lifecycle.v1");
        properties.getPublisher().setEnabled(true);
        properties.getPublisher().setSendTimeoutMs(100);
        event = new DeliveryOutboxEvent();
        event.setId(UUID.randomUUID());
        event.setAggregateId(UUID.randomUUID());
        event.setEventType("DELIVERY_OFFER_CREATED");
        event.setPayload(new ObjectMapper().createObjectNode());
        event.setClaimToken(UUID.randomUUID());
        event.setCreatedAt(Instant.now());
        publisher = new DeliveryOutboxEventPublisher(claims, states, properties, kafkaTemplate);
    }

    @Test
    void publisherMarksEventAfterKafkaAcknowledges() {
        when(claims.claimNextBatch()).thenReturn(List.of(event));
        when(kafkaTemplate.send("delivery.lifecycle.v1", event.getAggregateId().toString(), "{}"))
                .thenReturn(CompletableFuture.completedFuture(new SendResult<>(null, (RecordMetadata) null)));

        publisher.publishPendingEvents();

        verify(states).markPublished(event.getId(), event.getClaimToken());
    }

    @Test
    void failedPublishKeepsEventRetryable() {
        when(claims.claimNextBatch()).thenReturn(List.of(event));
        when(kafkaTemplate.send("delivery.lifecycle.v1", event.getAggregateId().toString(), "{}"))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker unavailable")));

        publisher.publishPendingEvents();

        verify(states).markFailed(eq(event.getId()), eq(event.getClaimToken()), any(Throwable.class));
    }

    @Test
    void retryAfterFailureCanBeMarkedPublished() {
        when(claims.claimNextBatch()).thenReturn(List.of(event));
        when(kafkaTemplate.send("delivery.lifecycle.v1", event.getAggregateId().toString(), "{}"))
                .thenReturn(CompletableFuture.completedFuture(new SendResult<>(null, (RecordMetadata) null)));

        publisher.publishPendingEvents();

        verify(states).markPublished(event.getId(), event.getClaimToken());
    }
}
