package com.khanh.fooddelivery.catalog_service.outbox;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

class OutboxEventPublisherTests {
    @Test
    void successfulBrokerAcknowledgementMarksEventPublished() {
        OutboxEventClaimService claimService = Mockito.mock(OutboxEventClaimService.class);
        OutboxEventStateService stateService = Mockito.mock(OutboxEventStateService.class);
        OutboxProperties properties = new OutboxProperties();
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = Mockito.mock(KafkaTemplate.class);
        OutboxEvent event = event();
        when(claimService.claimNextBatch()).thenReturn(List.of(event));
        CompletableFuture<SendResult<String, String>> result =
                CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(eq("catalog.events"), any(String.class), any(String.class)))
                .thenReturn(result);

        new OutboxEventPublisher(claimService, stateService, properties, kafkaTemplate)
                .publishPendingEvents();

        verify(stateService).markPublished(event.getId());
    }

    @Test
    void brokerFailureSchedulesRetryWithoutThrowingToBusinessCaller() {
        OutboxEventClaimService claimService = Mockito.mock(OutboxEventClaimService.class);
        OutboxEventStateService stateService = Mockito.mock(OutboxEventStateService.class);
        OutboxProperties properties = new OutboxProperties();
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = Mockito.mock(KafkaTemplate.class);
        OutboxEvent event = event();
        when(claimService.claimNextBatch()).thenReturn(List.of(event));
        CompletableFuture<SendResult<String, String>> result =
                CompletableFuture.failedFuture(new IllegalStateException("broker unavailable"));
        when(kafkaTemplate.send(eq("catalog.events"), any(String.class), any(String.class)))
                .thenReturn(result);

        new OutboxEventPublisher(claimService, stateService, properties, kafkaTemplate)
                .publishPendingEvents();

        verify(stateService).markFailed(eq(event.getId()), any(Exception.class));
    }

    @Test
    void boundedAcknowledgementTimeoutSchedulesRetry() {
        OutboxEventClaimService claimService = Mockito.mock(OutboxEventClaimService.class);
        OutboxEventStateService stateService = Mockito.mock(OutboxEventStateService.class);
        OutboxProperties properties = new OutboxProperties();
        properties.getPublisher().setSendTimeoutMs(1);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = Mockito.mock(KafkaTemplate.class);
        OutboxEvent event = event();
        when(claimService.claimNextBatch()).thenReturn(List.of(event));
        when(kafkaTemplate.send(eq("catalog.events"), any(String.class), any(String.class)))
                .thenReturn(new CompletableFuture<>());

        new OutboxEventPublisher(claimService, stateService, properties, kafkaTemplate)
                .publishPendingEvents();

        verify(stateService).markFailed(eq(event.getId()), any(Exception.class));
    }

    @Test
    void emptyClaimDoesNotSendPublishedEventsAgain() {
        OutboxEventClaimService claimService = Mockito.mock(OutboxEventClaimService.class);
        OutboxEventStateService stateService = Mockito.mock(OutboxEventStateService.class);
        OutboxProperties properties = new OutboxProperties();
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = Mockito.mock(KafkaTemplate.class);
        when(claimService.claimNextBatch()).thenReturn(List.of());

        new OutboxEventPublisher(claimService, stateService, properties, kafkaTemplate)
                .publishPendingEvents();

        verify(kafkaTemplate, never()).send(any(String.class), any(String.class), any(String.class));
        verify(stateService, never()).markPublished(any());
    }

    private OutboxEvent event() {
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setAggregateId(UUID.randomUUID());
        event.setEventType(CatalogEventType.CATALOG_ITEM_UPSERTED.name());
        event.setPayload(
                new ObjectMapper().createObjectNode().put("eventType", event.getEventType()));
        event.setStatus(OutboxStatus.PROCESSING);
        return event;
    }
}
