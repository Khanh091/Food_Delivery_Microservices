package com.khanh.fooddelivery.payment_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.payment_service.dto.request.PaymentWebhookRequest;
import com.khanh.fooddelivery.payment_service.entity.FinancialSnapshot;
import com.khanh.fooddelivery.payment_service.entity.Payment;
import com.khanh.fooddelivery.payment_service.model.PaymentMethod;
import com.khanh.fooddelivery.payment_service.model.PaymentProvider;
import com.khanh.fooddelivery.payment_service.model.PaymentStatus;
import com.khanh.fooddelivery.payment_service.provider.PaymentProviderGateway;
import com.khanh.fooddelivery.payment_service.repository.FinancialSnapshotRepository;
import com.khanh.fooddelivery.payment_service.repository.LedgerEntryRepository;
import com.khanh.fooddelivery.payment_service.repository.PaymentRepository;
import com.khanh.fooddelivery.payment_service.service.impl.PaymentTransactionServiceImpl;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class PaymentTransactionServiceTests {
    @Mock PaymentRepository payments;
    @Mock FinancialSnapshotRepository snapshots;
    @Mock LedgerEntryRepository ledgerEntries;
    @Mock LedgerService ledger;

    PaymentTransactionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PaymentTransactionServiceImpl(payments, snapshots, ledgerEntries, ledger,
                new PaymentStateMachine(), new FinancialSnapshotFactory(),
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void duplicateSuccessWebhookDoesNotDowngradeOrDuplicateCapture() {
        Payment payment = onlinePayment(PaymentStatus.PENDING);
        when(payments.findWithLockById(payment.getId())).thenReturn(Optional.of(payment));
        PaymentWebhookRequest request = webhook(payment, "SUCCESS");

        PaymentTransactionService.WebhookMutation first = service.applyVerifiedWebhook(request);
        PaymentTransactionService.WebhookMutation second = service.applyVerifiedWebhook(request);

        assertThat(first.payment().getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(second.payment().getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(second.notifyOrderPaid()).isTrue();
        verify(ledger, times(1)).record(any());
    }

    @Test
    void failureWebhookDoesNotDowngradePaidPayment() {
        Payment payment = onlinePayment(PaymentStatus.PAID);
        when(payments.findWithLockById(payment.getId())).thenReturn(Optional.of(payment));

        PaymentTransactionService.WebhookMutation mutation = service.applyVerifiedWebhook(webhook(payment, "FAILED"));

        assertThat(mutation.payment().getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(mutation.notifyOrderFailed()).isFalse();
        verify(ledger, never()).record(any());
    }

    @Test
    void duplicateFailureWebhookStillRetriesOrderNotification() {
        Payment payment = onlinePayment(PaymentStatus.FAILED);
        when(payments.findWithLockById(payment.getId())).thenReturn(Optional.of(payment));

        PaymentTransactionService.WebhookMutation mutation = service.applyVerifiedWebhook(webhook(payment, "FAILED"));

        assertThat(mutation.payment().getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(mutation.notifyOrderFailed()).isTrue();
    }

    @Test
    void cancelledWebhookMovesPendingPaymentToCancelled() {
        Payment payment = onlinePayment(PaymentStatus.PENDING);
        when(payments.findWithLockById(payment.getId())).thenReturn(Optional.of(payment));

        PaymentTransactionService.WebhookMutation mutation =
                service.applyVerifiedWebhook(webhook(payment, "CANCELLED"));

        assertThat(mutation.payment().getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(mutation.payment().getCancelledAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
        verify(ledger, never()).record(any());
    }

    @Test
    void cancelledWebhookCannotDowngradePaidPayment() {
        Payment payment = onlinePayment(PaymentStatus.PAID);
        when(payments.findWithLockById(payment.getId())).thenReturn(Optional.of(payment));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.applyVerifiedWebhook(webhook(payment, "CANCELLED")))
                .isInstanceOf(com.khanh.fooddelivery.payment_service.exception.PaymentException.class)
                .hasMessageContaining("cannot be cancelled");
    }

    private Payment onlinePayment(PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setOrderId(UUID.randomUUID());
        payment.setCustomerUserId(UUID.randomUUID());
        payment.setMethod(PaymentMethod.ONLINE);
        payment.setStatus(status);
        payment.setAmount(new BigDecimal("75000.00"));
        payment.setCurrency("VND");
        payment.setProvider(PaymentProvider.MOCK);
        payment.setProviderTransactionId("MOCK-TX");
        return payment;
    }

    private PaymentWebhookRequest webhook(Payment payment, String status) {
        return new PaymentWebhookRequest(payment.getId(), payment.getOrderId(), "MOCK-TX", status,
                payment.getAmount(), payment.getCurrency(), "signature");
    }
}
