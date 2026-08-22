package com.khanh.fooddelivery.order_service.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khanh.fooddelivery.order_service.entity.Order;
import com.khanh.fooddelivery.order_service.event.OrderConfirmedEvent;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderOutboxServiceImpl implements OrderOutboxService {

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishOrderConfirmed(Order order) {
        Instant occurredAt = clock.instant();
        UUID eventId = UUID.randomUUID();
        OrderConfirmedEvent event = new OrderConfirmedEvent(
                eventId,
                OrderConfirmedEvent.TYPE,
                occurredAt,
                order.getId(),
                OrderConfirmedEvent.VERSION,
                new OrderConfirmedEvent.Payload(
                        order.getId(),
                        order.getRestaurantId(),
                        order.getBranchId(),
                        order.getCustomerId(),
                        order.getRestaurantName(),
                        order.getBranchName(),
                        order.getAddressDisplayLabel(),
                        deliveryAddress(order),
                        order.getLatitude(),
                        order.getLongitude()
                )
        );

        OutboxEvent outbox = new OutboxEvent();
        outbox.setId(eventId);
        outbox.setAggregateType("ORDER");
        outbox.setAggregateId(order.getId());
        outbox.setEventType(OrderConfirmedEvent.TYPE);
        outbox.setPayload(objectMapper.valueToTree(event));
        outbox.setCreatedAt(occurredAt);
        outbox.setAttemptCount(0);
        repository.save(outbox);
    }

    private String deliveryAddress(Order order) {
        if (order.getFormattedAddress() != null && !order.getFormattedAddress().isBlank()) {
            return order.getFormattedAddress();
        }
        return String.join(", ", java.util.stream.Stream.of(
                        order.getAddressLine(), order.getWard(), order.getDistrict(), order.getCity())
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList());
    }
}
