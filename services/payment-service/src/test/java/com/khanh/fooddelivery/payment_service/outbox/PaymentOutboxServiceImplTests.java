package com.khanh.fooddelivery.payment_service.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.khanh.fooddelivery.payment_service.entity.Payment;
import com.khanh.fooddelivery.payment_service.event.PaymentLifecycleEvent;
import com.khanh.fooddelivery.payment_service.model.PaymentMethod;
import com.khanh.fooddelivery.payment_service.model.PaymentProvider;
import com.khanh.fooddelivery.payment_service.model.PaymentStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentOutboxServiceImplTests {

    @Mock
    private PaymentOutboxEventRepository repository;

    private PaymentOutboxServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PaymentOutboxServiceImpl(
                repository,
                new ObjectMapper().registerModule(new JavaTimeModule()),
                Clock.fixed(Instant.parse("2026-08-23T10:15:30Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void createsPaymentSucceededEnvelopeWithOnlyOrderProjectionData() {
        Payment payment = payment(PaymentStatus.PAID);
        when(repository.existsByAggregateTypeAndAggregateIdAndEventType(
                "PAYMENT", payment.getId(), PaymentLifecycleEvent.PAYMENT_SUCCEEDED))
                .thenReturn(false);

        service.publishPaymentSucceeded(payment);

        ArgumentCaptor<PaymentOutboxEvent> captor = ArgumentCaptor.forClass(PaymentOutboxEvent.class);
        verify(repository).save(captor.capture());
        PaymentOutboxEvent outbox = captor.getValue();
        assertThat(outbox.getAggregateId()).isEqualTo(payment.getId());
        assertThat(outbox.getEventType()).isEqualTo(PaymentLifecycleEvent.PAYMENT_SUCCEEDED);
        assertThat(outbox.getPublishedAt()).isNull();
        assertThat(outbox.getPayload().get("eventId").asText()).isNotBlank();
        assertThat(outbox.getPayload().get("payload").get("paymentId").asText())
                .isEqualTo(payment.getId().toString());
        assertThat(outbox.getPayload().get("payload").get("orderId").asText())
                .isEqualTo(payment.getOrderId().toString());
        assertThat(outbox.getPayload().get("payload").get("paymentStatus").asText())
                .isEqualTo(PaymentStatus.PAID.name());
        assertThat(outbox.getPayload().get("payload").get("providerTransactionId")).isNull();
    }

    @Test
    void duplicateSemanticEventDoesNotCreateAnotherOutboxRow() {
        Payment payment = payment(PaymentStatus.FAILED);
        when(repository.existsByAggregateTypeAndAggregateIdAndEventType(
                "PAYMENT", payment.getId(), PaymentLifecycleEvent.PAYMENT_FAILED))
                .thenReturn(true);

        service.publishPaymentFailed(payment);

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any(PaymentOutboxEvent.class));
    }

    private Payment payment(PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setOrderId(UUID.randomUUID());
        payment.setCustomerUserId(UUID.randomUUID());
        payment.setMethod(PaymentMethod.ONLINE);
        payment.setStatus(status);
        payment.setAmount(new BigDecimal("75000.00"));
        payment.setCurrency("VND");
        payment.setProvider(PaymentProvider.MOCK);
        payment.setProviderTransactionId("DO-NOT-PUBLISH");
        return payment;
    }
}
