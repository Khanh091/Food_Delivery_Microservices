package com.khanh.fooddelivery.payment_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.payment_service.dto.request.CashActionRequest;
import com.khanh.fooddelivery.payment_service.entity.FinancialSnapshot;
import com.khanh.fooddelivery.payment_service.entity.Payment;
import com.khanh.fooddelivery.payment_service.model.PaymentMethod;
import com.khanh.fooddelivery.payment_service.model.PaymentProvider;
import com.khanh.fooddelivery.payment_service.model.PaymentStatus;
import com.khanh.fooddelivery.payment_service.repository.FinancialSnapshotRepository;
import com.khanh.fooddelivery.payment_service.repository.PaymentRepository;
import com.khanh.fooddelivery.payment_service.service.impl.CodPaymentServiceImpl;
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
class CodPaymentServiceTests {
    @Mock PaymentRepository payments;
    @Mock FinancialSnapshotRepository snapshots;
    @Mock LedgerService ledger;

    CodPaymentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CodPaymentServiceImpl(payments, snapshots, ledger, new PaymentStateMachine(),
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void advanceAndCashCollectionAreIdempotentAndBindDeliveryAndDriver() {
        UUID orderId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        Payment payment = payment(orderId);
        FinancialSnapshot snapshot = snapshot(orderId);
        when(payments.findByOrderId(orderId)).thenReturn(Optional.of(payment));
        when(payments.findWithLockById(payment.getId())).thenReturn(Optional.of(payment));
        when(snapshots.findByOrderId(orderId)).thenReturn(Optional.of(snapshot));

        CashActionRequest request = new CashActionRequest(orderId, deliveryId, driverId, "advance");
        service.confirmRestaurantAdvance(request);
        service.confirmRestaurantAdvance(request);
        service.collectCash(new CashActionRequest(orderId, deliveryId, driverId, "cash"));
        service.collectCash(new CashActionRequest(orderId, deliveryId, driverId, "cash"));

        assertThat(payment.getDeliveryId()).isEqualTo(deliveryId);
        assertThat(payment.getDriverId()).isEqualTo(driverId);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COLLECTED);
        verify(ledger, times(2)).record(any());
    }

    private Payment payment(UUID orderId) {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setOrderId(orderId);
        payment.setCustomerUserId(UUID.randomUUID());
        payment.setRestaurantId(UUID.randomUUID());
        payment.setMethod(PaymentMethod.COD);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(new BigDecimal("75000.00"));
        payment.setCurrency("VND");
        payment.setProvider(PaymentProvider.COD);
        return payment;
    }

    private FinancialSnapshot snapshot(UUID orderId) {
        FinancialSnapshot snapshot = new FinancialSnapshot();
        snapshot.setOrderId(orderId);
        snapshot.setFoodGrossAmount(new BigDecimal("50000.00"));
        snapshot.setCurrency("VND");
        snapshot.setCustomerPayableAmount(new BigDecimal("75000.00"));
        return snapshot;
    }
}
