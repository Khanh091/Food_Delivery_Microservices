package com.khanh.fooddelivery.order_service.outbox;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
class OutboxEventPublisherTests {

    @Mock
    private OutboxEventClaimService claims;
    @Mock
    private OutboxEventStateService states;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxProperties properties;
    private OutboxEvent event;
    private OutboxEventPublisher publisher;

    @BeforeEach
    void setUp() {
        properties = new OutboxProperties();
        properties.setTopic("order.lifecycle.v1");
        properties.getPublisher().setEnabled(true);
        properties.getPublisher().setSendTimeoutMs(100);
        event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setAggregateId(UUID.randomUUID());
        event.setEventType("ORDER_CONFIRMED");
        event.setPayload(new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode());
        event.setClaimToken(UUID.randomUUID());
        event.setCreatedAt(Instant.now());
        publisher = new OutboxEventPublisher(claims, states, properties, kafkaTemplate);
    }

    @Test
    void publishedEventIsMarkedOnlyAfterKafkaAcknowledges() {
        when(claims.claimNextBatch()).thenReturn(List.of(event));
        CompletableFuture<SendResult<String, String>> acknowledged = CompletableFuture.completedFuture(
                new SendResult<>(null, (RecordMetadata) null));
        when(kafkaTemplate.send("order.lifecycle.v1", event.getAggregateId().toString(), "{}"))
                .thenReturn(acknowledged);

        publisher.publishPendingEvents();

        verify(states).markPublished(event.getId(), event.getClaimToken());
    }

    @Test
    void failedPublishRemainsRetryable() {
        when(claims.claimNextBatch()).thenReturn(List.of(event));
        when(kafkaTemplate.send("order.lifecycle.v1", event.getAggregateId().toString(), "{}"))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker unavailable")));

        publisher.publishPendingEvents();

        verify(states).markFailed(eq(event.getId()), eq(event.getClaimToken()), any(Throwable.class));
    }
}
