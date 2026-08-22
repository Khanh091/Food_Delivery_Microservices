package com.khanh.fooddelivery.delivery_service.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.khanh.fooddelivery.delivery_service.service.DeliveryCreationService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderConfirmedConsumerTests {

    @Mock
    private DeliveryCreationService deliveryCreation;

    private OrderConfirmedConsumer consumer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        consumer = new OrderConfirmedConsumer(objectMapper, deliveryCreation);
    }

    @Test
    void validEventIsPassedToIdempotentDeliveryCreationBoundary() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderConfirmedEvent event = new OrderConfirmedEvent(
                UUID.randomUUID(), OrderConfirmedEvent.TYPE, Instant.now(), orderId, 1,
                new OrderConfirmedEvent.Payload(
                        orderId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        "Restaurant", "Branch", "Nhà", "97 Man Thien",
                        new BigDecimal("10.8"), new BigDecimal("106.7")));

        consumer.consume(objectMapper.writeValueAsString(event));

        verify(deliveryCreation).ensureMatching(any(OrderConfirmedEvent.Payload.class));
    }
}
