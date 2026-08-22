package com.khanh.fooddelivery.payment_service.service.impl;

import com.khanh.fooddelivery.payment_service.dto.response.FinancialFactsResponse;
import com.khanh.fooddelivery.payment_service.entity.FinancialSnapshot;
import com.khanh.fooddelivery.payment_service.entity.Payment;
import com.khanh.fooddelivery.payment_service.exception.PaymentException;
import com.khanh.fooddelivery.payment_service.model.FinancialSnapshotStatus;
import com.khanh.fooddelivery.payment_service.model.LedgerDirection;
import com.khanh.fooddelivery.payment_service.model.LedgerEntryType;
import com.khanh.fooddelivery.payment_service.model.PaymentMethod;
import com.khanh.fooddelivery.payment_service.repository.FinancialSnapshotRepository;
import com.khanh.fooddelivery.payment_service.repository.PaymentRepository;
import com.khanh.fooddelivery.payment_service.service.FinancialFactsAssembler;
import com.khanh.fooddelivery.payment_service.service.FinancialService;
import com.khanh.fooddelivery.payment_service.service.LedgerCommand;
import com.khanh.fooddelivery.payment_service.service.LedgerService;
import com.khanh.fooddelivery.payment_service.service.PaymentStateMachine;
import com.khanh.fooddelivery.payment_service.outbox.PaymentOutboxService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FinancialServiceImpl implements FinancialService {
    private final PaymentRepository payments;
    private final FinancialSnapshotRepository snapshots;
    private final LedgerService ledger;
    private final PaymentStateMachine stateMachine;
    private final FinancialFactsAssembler factsAssembler;
    private final PaymentOutboxService paymentOutbox;

    @Override
    @Transactional(readOnly = true)
    public FinancialFactsResponse facts(UUID orderId) {
        Payment payment = payments.findByOrderId(orderId).orElseThrow(() -> missing("Payment not found"));
        FinancialSnapshot snapshot = snapshots.findByOrderId(orderId)
                .orElseThrow(() -> missing("Financial snapshot not found"));
        return factsAssembler.toResponse(payment, snapshot);
    }

    @Override
    @Transactional
    public PaymentMethod completeDelivery(UUID orderId, UUID deliveryId, UUID driverId) {
        Payment payment = payments.findByOrderId(orderId).orElseThrow(() -> missing("Payment not found"));
        payment = payments.findWithLockById(payment.getId()).orElseThrow(() -> missing("Payment not found"));
        FinancialSnapshot snapshot = snapshots.findByOrderId(orderId)
                .orElseThrow(() -> missing("Financial snapshot not found"));
        bindDelivery(payment, deliveryId);
        bindDriver(payment, snapshot, driverId);

        if (snapshot.getStatus() == FinancialSnapshotStatus.REVERSED
                || snapshot.getStatus() == FinancialSnapshotStatus.CANCELLED) {
            throw conflict("Financial snapshot has already been reversed");
        }
        if (snapshot.getStatus() == FinancialSnapshotStatus.FINALIZED) {
            return payment.getMethod();
        }

        if (payment.getMethod() == PaymentMethod.COD) {
            if (payment.getRestaurantAdvanceConfirmedAt() == null || payment.getCashCollectedAt() == null) {
                throw conflict("COD cash actions are incomplete");
            }
            if (payment.getStatus() != com.khanh.fooddelivery.payment_service.model.PaymentStatus.COLLECTED) {
                throw conflict("COD cash has not been collected");
            }
            ledger.record(new LedgerCommand("RESTAURANT", payment.getRestaurantId(), orderId, payment.getId(),
                    LedgerEntryType.RESTAURANT_COMMISSION_RECEIVABLE, LedgerDirection.CREDIT,
                    snapshot.getRestaurantCommissionAmount(), snapshot.getCurrency(),
                    "settlement:" + payment.getId() + ":restaurant-commission"));
            ledger.record(new LedgerCommand("DRIVER", payment.getDriverId(), orderId, payment.getId(),
                    LedgerEntryType.DRIVER_COMMISSION_RECEIVABLE, LedgerDirection.CREDIT,
                    snapshot.getDriverCommissionAmount(), snapshot.getCurrency(),
                    "settlement:" + payment.getId() + ":driver-commission"));
        } else {
            stateMachine.ensureOnlinePaid(payment);
            ledger.record(new LedgerCommand("RESTAURANT", payment.getRestaurantId(), orderId, payment.getId(),
                    LedgerEntryType.RESTAURANT_PAYABLE_CREATED, LedgerDirection.CREDIT,
                    snapshot.getRestaurantNetAmount(), snapshot.getCurrency(),
                    "settlement:" + payment.getId() + ":restaurant-payable"));
            ledger.record(new LedgerCommand("DRIVER", payment.getDriverId(), orderId, payment.getId(),
                    LedgerEntryType.DRIVER_PAYABLE_CREATED, LedgerDirection.CREDIT,
                    snapshot.getDriverNetAmount(), snapshot.getCurrency(),
                    "settlement:" + payment.getId() + ":driver-payable"));
        }
        ledger.record(new LedgerCommand("PLATFORM", null, orderId, payment.getId(),
                LedgerEntryType.PLATFORM_REVENUE_RECOGNIZED, LedgerDirection.CREDIT,
                snapshot.getPlatformRevenueAmount(), snapshot.getCurrency(),
                "settlement:" + payment.getId() + ":platform-revenue"));
        snapshot.setStatus(FinancialSnapshotStatus.FINALIZED);
        if (payment.getMethod() == PaymentMethod.COD) {
            paymentOutbox.publishPaymentCollected(payment);
        }
        return payment.getMethod();
    }

    private void bindDelivery(Payment payment, UUID deliveryId) {
        if (deliveryId == null) {
            throw invalid("Canonical delivery id is required");
        }
        if (payment.getDeliveryId() != null && !payment.getDeliveryId().equals(deliveryId)) {
            throw conflict("Payment is bound to another delivery");
        }
        payment.setDeliveryId(deliveryId);
    }

    private void bindDriver(Payment payment, FinancialSnapshot snapshot, UUID driverId) {
        if (driverId == null) {
            throw invalid("Canonical driver id is required");
        }
        if (payment.getDriverId() != null && !payment.getDriverId().equals(driverId)) {
            throw new PaymentException("PAYMENT_403", HttpStatus.FORBIDDEN,
                    "Payment is assigned to another driver");
        }
        payment.setDriverId(driverId);
        if (snapshot.getDriverId() != null && !snapshot.getDriverId().equals(driverId)) {
            throw new PaymentException("PAYMENT_403", HttpStatus.FORBIDDEN,
                    "Financial snapshot is assigned to another driver");
        }
        snapshot.setDriverId(driverId);
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
