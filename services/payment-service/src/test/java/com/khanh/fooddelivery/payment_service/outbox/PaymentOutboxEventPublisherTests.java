package com.khanh.fooddelivery.payment_service.outbox;

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
class PaymentOutboxEventPublisherTests {

    @Mock
    private PaymentOutboxEventClaimService claims;
    @Mock
    private PaymentOutboxEventStateService states;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private PaymentOutboxProperties properties;
    private PaymentOutboxEvent event;
    private PaymentOutboxEventPublisher publisher;

    @BeforeEach
    void setUp() {
        properties = new PaymentOutboxProperties();
        properties.setTopic("payment.lifecycle.v1");
        properties.getPublisher().setEnabled(true);
        properties.getPublisher().setSendTimeoutMs(100);
        event = new PaymentOutboxEvent();
        event.setId(UUID.randomUUID());
        event.setAggregateId(UUID.randomUUID());
        event.setEventType("PAYMENT_SUCCEEDED");
        event.setPayload(new ObjectMapper().createObjectNode());
        event.setClaimToken(UUID.randomUUID());
        event.setCreatedAt(Instant.now());
        publisher = new PaymentOutboxEventPublisher(claims, states, properties, kafkaTemplate);
    }

    @Test
    void publisherMarksEventAfterKafkaAcknowledges() {
        when(claims.claimNextBatch()).thenReturn(List.of(event));
        CompletableFuture<SendResult<String, String>> acknowledged = CompletableFuture.completedFuture(
                new SendResult<>(null, (RecordMetadata) null));
        when(kafkaTemplate.send("payment.lifecycle.v1", event.getAggregateId().toString(), "{}"))
                .thenReturn(acknowledged);

        publisher.publishPendingEvents();

        verify(states).markPublished(event.getId(), event.getClaimToken());
    }

    @Test
    void failedPublishRemainsUnpublishedAndRetryable() {
        when(claims.claimNextBatch()).thenReturn(List.of(event));
        when(kafkaTemplate.send("payment.lifecycle.v1", event.getAggregateId().toString(), "{}"))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker unavailable")));

        publisher.publishPendingEvents();

        verify(states).markFailed(eq(event.getId()), eq(event.getClaimToken()), any(Throwable.class));
    }

    @Test
    void retryAfterFailureCanBeMarkedPublished() {
        when(claims.claimNextBatch()).thenReturn(List.of(event));
        when(kafkaTemplate.send("payment.lifecycle.v1", event.getAggregateId().toString(), "{}"))
                .thenReturn(CompletableFuture.completedFuture(new SendResult<>(null, (RecordMetadata) null)));

        publisher.publishPendingEvents();

        verify(states).markPublished(event.getId(), event.getClaimToken());
    }
}
