package com.khanh.fooddelivery.restaurant_service.outbox;

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
    private final OutboxEventRepository repository;
    private final OutboxAggregateVersionService versions;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(RestaurantEventType type, String aggregateType, UUID aggregateId, Object data) {
        UUID eventId = UUID.randomUUID(); Instant now = clock.instant();
        long aggregateVersion = versions.nextVersion(aggregateType, aggregateId);
        OutboxEvent event = new OutboxEvent();
        event.setId(eventId); event.setAggregateType(aggregateType); event.setAggregateId(aggregateId);
        event.setEventType(type.name()); event.setEventVersion(EVENT_VERSION); event.setAggregateVersion(aggregateVersion);
        event.setPayload(objectMapper.valueToTree(new DomainEventEnvelope<>(eventId, type.name(), EVENT_VERSION,
                aggregateType, aggregateId, aggregateVersion, now, data)));
        event.setStatus(OutboxStatus.PENDING); event.setRetryCount(0); event.setCreatedAt(now); event.setUpdatedAt(now);
        repository.save(event);
    }
}
