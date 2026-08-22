package com.khanh.fooddelivery.payment_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.payment_service.entity.FinancialSnapshot;
import com.khanh.fooddelivery.payment_service.entity.Payment;
import com.khanh.fooddelivery.payment_service.model.FinancialSnapshotStatus;
import com.khanh.fooddelivery.payment_service.model.PaymentMethod;
import com.khanh.fooddelivery.payment_service.model.PaymentProvider;
import com.khanh.fooddelivery.payment_service.model.PaymentStatus;
import com.khanh.fooddelivery.payment_service.repository.FinancialSnapshotRepository;
import com.khanh.fooddelivery.payment_service.repository.PaymentRepository;
import com.khanh.fooddelivery.payment_service.service.impl.FinancialServiceImpl;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class FinancialServiceTests {
    @Mock PaymentRepository payments;
    @Mock FinancialSnapshotRepository snapshots;
    @Mock LedgerService ledger;

    FinancialServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FinancialServiceImpl(payments, snapshots, ledger, new PaymentStateMachine(),
                new FinancialFactsAssembler());
    }

    @Test
    void onlineCompletionCreatesThirtyFiveAndSeventeenPointFivePayables() {
        UUID orderId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        Payment payment = payment(orderId, PaymentMethod.ONLINE, PaymentStatus.PAID);
        FinancialSnapshot snapshot = snapshot(orderId);
        when(payments.findByOrderId(orderId)).thenReturn(Optional.of(payment));
        when(payments.findWithLockById(payment.getId())).thenReturn(Optional.of(payment));
        when(snapshots.findByOrderId(orderId)).thenReturn(Optional.of(snapshot));

        service.completeDelivery(orderId, deliveryId, driverId);

        assertThat(snapshot.getStatus()).isEqualTo(FinancialSnapshotStatus.FINALIZED);
        verify(ledger, times(3)).record(any(LedgerCommand.class));
    }

    @Test
    void secondCompletionIsIdempotentAndDoesNotAddLedgerEntries() {
        UUID orderId = UUID.randomUUID();
        Payment payment = payment(orderId, PaymentMethod.ONLINE, PaymentStatus.PAID);
        FinancialSnapshot snapshot = snapshot(orderId);
        snapshot.setStatus(FinancialSnapshotStatus.FINALIZED);
        when(payments.findByOrderId(orderId)).thenReturn(Optional.of(payment));
        when(payments.findWithLockById(payment.getId())).thenReturn(Optional.of(payment));
        when(snapshots.findByOrderId(orderId)).thenReturn(Optional.of(snapshot));

        service.completeDelivery(orderId, UUID.randomUUID(), UUID.randomUUID());

        verify(ledger, times(0)).record(any(LedgerCommand.class));
    }

    private Payment payment(UUID orderId, PaymentMethod method, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setOrderId(orderId);
        payment.setCustomerUserId(UUID.randomUUID());
        payment.setRestaurantId(UUID.randomUUID());
        payment.setMethod(method);
        payment.setStatus(status);
        payment.setAmount(new BigDecimal("75000.00"));
        payment.setCurrency("VND");
        payment.setProvider(PaymentProvider.MOCK);
        return payment;
    }

    private FinancialSnapshot snapshot(UUID orderId) {
        FinancialSnapshot snapshot = new FinancialSnapshot();
        snapshot.setOrderId(orderId);
        snapshot.setStatus(FinancialSnapshotStatus.OPEN);
        snapshot.setCurrency("VND");
        snapshot.setRestaurantCommissionAmount(new BigDecimal("15000.00"));
        snapshot.setRestaurantNetAmount(new BigDecimal("35000.00"));
        snapshot.setDriverCommissionAmount(new BigDecimal("7500.00"));
        snapshot.setDriverNetAmount(new BigDecimal("17500.00"));
        snapshot.setPlatformRevenueAmount(new BigDecimal("22500.00"));
        snapshot.setFoodGrossAmount(new BigDecimal("50000.00"));
        snapshot.setDeliveryGrossAmount(new BigDecimal("25000.00"));
        snapshot.setCustomerPayableAmount(new BigDecimal("75000.00"));
        return snapshot;
    }
}
