package com.khanh.fooddelivery.delivery_service.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khanh.fooddelivery.delivery_service.event.DeliveryLifecycleEvent;
import com.khanh.fooddelivery.delivery_service.model.DeliveryOffer;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeliveryOutboxServiceImpl implements DeliveryOutboxService {

    private static final String AGGREGATE_TYPE = "DELIVERY_OFFER";

    private final DeliveryOutboxEventRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishDeliveryOfferCreated(DeliveryOffer offer) {
        if (repository.existsByAggregateTypeAndAggregateIdAndEventType(
                AGGREGATE_TYPE,
                offer.getId(),
                DeliveryLifecycleEvent.DELIVERY_OFFER_CREATED
        )) {
            return;
        }

        Instant occurredAt = clock.instant();
        UUID eventId = UUID.randomUUID();
        DeliveryLifecycleEvent event = new DeliveryLifecycleEvent(
                eventId,
                DeliveryLifecycleEvent.DELIVERY_OFFER_CREATED,
                occurredAt,
                offer.getId(),
                DeliveryLifecycleEvent.VERSION,
                new DeliveryLifecycleEvent.Payload(
                        offer.getId(),
                        offer.getDeliveryId(),
                        offer.getDriverId(),
                        offer.getExpiresAt()
                )
        );

        DeliveryOutboxEvent outbox = new DeliveryOutboxEvent();
        outbox.setId(eventId);
        outbox.setAggregateType(AGGREGATE_TYPE);
        outbox.setAggregateId(offer.getId());
        outbox.setEventType(DeliveryLifecycleEvent.DELIVERY_OFFER_CREATED);
        outbox.setPayload(objectMapper.valueToTree(event));
        outbox.setCreatedAt(occurredAt);
        outbox.setAttemptCount(0);
        repository.save(outbox);
    }
}
