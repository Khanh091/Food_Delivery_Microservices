package com.khanh.fooddelivery.payment_service.service.impl;

import com.khanh.fooddelivery.payment_service.dto.request.CashActionRequest;
import com.khanh.fooddelivery.payment_service.entity.FinancialSnapshot;
import com.khanh.fooddelivery.payment_service.entity.Payment;
import com.khanh.fooddelivery.payment_service.exception.PaymentException;
import com.khanh.fooddelivery.payment_service.model.LedgerDirection;
import com.khanh.fooddelivery.payment_service.model.LedgerEntryType;
import com.khanh.fooddelivery.payment_service.model.PaymentStatus;
import com.khanh.fooddelivery.payment_service.repository.FinancialSnapshotRepository;
import com.khanh.fooddelivery.payment_service.repository.PaymentRepository;
import com.khanh.fooddelivery.payment_service.service.CodPaymentService;
import com.khanh.fooddelivery.payment_service.service.LedgerCommand;
import com.khanh.fooddelivery.payment_service.service.LedgerService;
import com.khanh.fooddelivery.payment_service.service.PaymentStateMachine;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CodPaymentServiceImpl implements CodPaymentService {
    private final PaymentRepository payments;
    private final FinancialSnapshotRepository snapshots;
    private final LedgerService ledger;
    private final PaymentStateMachine stateMachine;
    private final Clock clock;

    @Override
    @Transactional
    public void confirmRestaurantAdvance(CashActionRequest request) {
        Payment payment = lock(request.orderId());
        stateMachine.ensureCod(payment, "Restaurant advance");
        stateMachine.ensureActiveForCash(payment);
        bindDeliveryAndDriver(payment, request);
        if (payment.getRestaurantAdvanceConfirmedAt() != null) {
            return;
        }

        FinancialSnapshot snapshot = snapshot(request.orderId());
        payment.setRestaurantAdvanceConfirmedAt(Instant.now(clock));
        ledger.record(new LedgerCommand("DRIVER", request.driverId(), payment.getOrderId(), payment.getId(),
                LedgerEntryType.DRIVER_RESTAURANT_ADVANCE, LedgerDirection.DEBIT,
                snapshot.getFoodGrossAmount(), snapshot.getCurrency(),
                "cod:" + payment.getId() + ":advance"));
    }

    @Override
    @Transactional
    public void collectCash(CashActionRequest request) {
        Payment payment = lock(request.orderId());
        stateMachine.ensureCod(payment, "Cash collection");
        stateMachine.ensureActiveForCash(payment);
        bindDeliveryAndDriver(payment, request);
        if (payment.getRestaurantAdvanceConfirmedAt() == null) {
            throw conflict("Restaurant advance must be confirmed first");
        }
        if (payment.getCashCollectedAt() != null) {
            return;
        }

        FinancialSnapshot snapshot = snapshot(request.orderId());
        payment.setCashCollectedAt(Instant.now(clock));
        payment.setCollectedAt(payment.getCashCollectedAt());
        payment.setStatus(PaymentStatus.COLLECTED);
        ledger.record(new LedgerCommand("DRIVER", request.driverId(), payment.getOrderId(), payment.getId(),
                LedgerEntryType.DRIVER_CUSTOMER_CASH_COLLECTED, LedgerDirection.CREDIT,
                snapshot.getCustomerPayableAmount(), snapshot.getCurrency(),
                "cod:" + payment.getId() + ":cash-collected"));
    }

    private void bindDeliveryAndDriver(Payment payment, CashActionRequest request) {
        if (request.deliveryId() == null) {
            throw invalid("Canonical delivery id is required");
        }
        if (payment.getDeliveryId() != null && !payment.getDeliveryId().equals(request.deliveryId())) {
            throw conflict("Payment is bound to another delivery");
        }
        if (request.driverId() == null) {
            throw invalid("Canonical driver id is required");
        }
        if (payment.getDriverId() != null && !payment.getDriverId().equals(request.driverId())) {
            throw new PaymentException("PAYMENT_403", HttpStatus.FORBIDDEN,
                    "Payment is assigned to another driver");
        }
        payment.setDeliveryId(request.deliveryId());
        payment.setDriverId(request.driverId());
    }

    private Payment lock(java.util.UUID orderId) {
        Payment payment = payments.findByOrderId(orderId).orElseThrow(() -> missing("Payment not found"));
        return payments.findWithLockById(payment.getId()).orElseThrow(() -> missing("Payment not found"));
    }

    private FinancialSnapshot snapshot(java.util.UUID orderId) {
        return snapshots.findByOrderId(orderId).orElseThrow(() -> missing("Financial snapshot not found"));
    }

    private PaymentException missing(String message) {
        return new PaymentException("PAYMENT_404", HttpStatus.NOT_FOUND, message);
    }

    private PaymentException invalid(String message) {
        return new PaymentException("PAYMENT_400", HttpStatus.BAD_REQUEST, message);
    }

    private PaymentException conflict(String message) {
        return new PaymentException("PAYMENT_409", HttpStatus.CONFLICT, message);
    }
}
