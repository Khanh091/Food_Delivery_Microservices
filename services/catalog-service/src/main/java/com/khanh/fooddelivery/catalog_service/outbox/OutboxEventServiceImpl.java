package com.khanh.fooddelivery.catalog_service.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxEventServiceImpl implements OutboxEventService {
    private static final int EVENT_VERSION = 2;

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxAggregateVersionService aggregateVersionService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(
            CatalogEventType eventType, String aggregateType, UUID aggregateId, Object data) {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = clock.instant();
        long aggregateVersion = aggregateVersionService.nextVersion(aggregateType, aggregateId);
        JsonNode payload =
                objectMapper.valueToTree(
                        new DomainEventEnvelope<>(
                                eventId,
                                eventType.name(),
                                EVENT_VERSION,
                                aggregateType,
                                aggregateId,
                                aggregateVersion,
                                occurredAt,
                                data));

        OutboxEvent event = new OutboxEvent();
        event.setId(eventId);
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setEventType(eventType.name());
        event.setEventVersion(EVENT_VERSION);
        event.setAggregateVersion(aggregateVersion);
        event.setPayload(payload);
        event.setStatus(OutboxStatus.PENDING);
        event.setRetryCount(0);
        event.setCreatedAt(occurredAt);
        event.setUpdatedAt(occurredAt);
        outboxEventRepository.save(event);
    }
}
