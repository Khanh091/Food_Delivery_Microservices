package com.khanh.fooddelivery.order_service.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.khanh.fooddelivery.order_service.entity.Order;
import com.khanh.fooddelivery.order_service.event.OrderConfirmedEvent;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class OrderOutboxServiceImplTests {

    private static final UUID ORDER_ID = UUID.randomUUID();

    @Mock
    private OutboxEventRepository repository;

    private OrderOutboxServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrderOutboxServiceImpl(
                repository,
                new ObjectMapper().registerModule(new JavaTimeModule()),
                Clock.fixed(Instant.parse("2026-08-22T10:15:30Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void createsOrderConfirmedEnvelopeWithDeliverySnapshot() {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setRestaurantId(UUID.randomUUID());
        order.setBranchId(UUID.randomUUID());
        order.setCustomerId(UUID.randomUUID());
        order.setRestaurantName("Restaurant");
        order.setBranchName("Branch");
        order.setAddressDisplayLabel("Nhà");
        order.setFormattedAddress("97 Man Thien, Thu Duc");
        order.setLatitude(new BigDecimal("10.8000"));
        order.setLongitude(new BigDecimal("106.7500"));

        service.publishOrderConfirmed(order);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());
        OutboxEvent outbox = captor.getValue();
        assertThat(outbox.getAggregateId()).isEqualTo(ORDER_ID);
        assertThat(outbox.getEventType()).isEqualTo(OrderConfirmedEvent.TYPE);
        assertThat(outbox.getPublishedAt()).isNull();
        assertThat(outbox.getPayload().get("payload").get("customerAddress").asText())
                .isEqualTo("97 Man Thien, Thu Duc");
        assertThat(outbox.getPayload().get("eventType").asText()).isEqualTo(OrderConfirmedEvent.TYPE);
    }
}
