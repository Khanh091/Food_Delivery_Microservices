package com.khanh.fooddelivery.payment_service.service.impl;

import com.khanh.fooddelivery.payment_service.dto.request.InternalCreatePaymentRequest;
import com.khanh.fooddelivery.payment_service.dto.request.PaymentWebhookRequest;
import com.khanh.fooddelivery.payment_service.entity.FinancialSnapshot;
import com.khanh.fooddelivery.payment_service.entity.Payment;
import com.khanh.fooddelivery.payment_service.exception.PaymentException;
import com.khanh.fooddelivery.payment_service.model.LedgerDirection;
import com.khanh.fooddelivery.payment_service.model.LedgerEntryType;
import com.khanh.fooddelivery.payment_service.model.PaymentMethod;
import com.khanh.fooddelivery.payment_service.model.PaymentProvider;
import com.khanh.fooddelivery.payment_service.model.PaymentStatus;
import com.khanh.fooddelivery.payment_service.model.FinancialSnapshotStatus;
import com.khanh.fooddelivery.payment_service.provider.PaymentProviderGateway;
import com.khanh.fooddelivery.payment_service.repository.FinancialSnapshotRepository;
import com.khanh.fooddelivery.payment_service.repository.LedgerEntryRepository;
import com.khanh.fooddelivery.payment_service.repository.PaymentRepository;
import com.khanh.fooddelivery.payment_service.service.FinancialBreakdown;
import com.khanh.fooddelivery.payment_service.service.FinancialSnapshotFactory;
import com.khanh.fooddelivery.payment_service.service.LedgerCommand;
import com.khanh.fooddelivery.payment_service.service.LedgerService;
import com.khanh.fooddelivery.payment_service.service.PaymentStateMachine;
import com.khanh.fooddelivery.payment_service.service.PaymentTransactionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentTransactionServiceImpl implements PaymentTransactionService {
    private static final int MONEY_SCALE = 2;

    private final PaymentRepository payments;
    private final FinancialSnapshotRepository snapshots;
    private final LedgerEntryRepository ledgerEntries;
    private final LedgerService ledger;
    private final PaymentStateMachine stateMachine;
    private final FinancialSnapshotFactory snapshotFactory;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findExisting(UUID orderId, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Payment keyed = payments.findByIdempotencyKey(idempotencyKey).orElse(null);
            if (keyed != null) {
                if (!keyed.getOrderId().equals(orderId)) {
                    throw conflict("Payment idempotency key is already used");
                }
                return Optional.of(keyed);
            }
        }
        return payments.findByOrderId(orderId);
    }

    @Override
    @Transactional
    public Payment createPending(InternalCreatePaymentRequest request, FinancialBreakdown breakdown) {
        if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            Payment keyed = payments.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
            if (keyed != null) {
                if (!keyed.getOrderId().equals(request.orderId())) {
                    throw conflict("Payment idempotency key is already used");
                }
                return keyed;
            }
        }
        Payment existing = payments.findByOrderId(request.orderId()).orElse(null);
        if (existing != null) {
            return existing;
        }

        FinancialSnapshot snapshot = snapshotFactory.create(request, breakdown);
        snapshots.save(snapshot);

        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setOrderId(request.orderId());
        payment.setIdempotencyKey(request.idempotencyKey());
        payment.setCustomerUserId(request.customerUserId());
        payment.setRestaurantId(request.restaurantId());
        payment.setMethod(request.method() == null ? PaymentMethod.COD : request.method());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(breakdown.customerPayableAmount());
        payment.setCurrency(breakdown.currency());
        payment.setProvider(payment.getMethod() == PaymentMethod.COD ? PaymentProvider.COD : PaymentProvider.MOCK);
        return payments.save(payment);
    }

    @Override
    @Transactional
    public Payment attachProviderTransaction(UUID paymentId, PaymentProviderGateway.ProviderTransaction transaction) {
        if (transaction == null || transaction.transactionId() == null || transaction.transactionId().isBlank()) {
            throw invalid("Provider transaction reference is required");
        }
        Payment payment = lockById(paymentId);
        payment.setProviderTransactionId(transaction.transactionId());
        payment.setProviderReference(transaction.reference());
        return payment;
    }

    @Override
    @Transactional
    public Payment beginRetry(UUID paymentId) {
        Payment payment = lockById(paymentId);
        stateMachine.ensureOnlineRetryable(payment);
        payment.setStatus(PaymentStatus.PROCESSING);
        payment.setFailureCode(null);
        payment.setFailureMessage(null);
        return payment;
    }

    @Override
    @Transactional
    public Payment markRetryFailed(UUID paymentId, String failureMessage) {
        Payment payment = lockById(paymentId);
        if (payment.getStatus() == PaymentStatus.PROCESSING) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureCode("PROVIDER_CREATE_FAILED");
            payment.setFailureMessage(failureMessage);
        }
        return payment;
    }

    @Override
    @Transactional
    public Payment markProviderCreateFailed(UUID paymentId, String failureMessage) {
        Payment payment = lockById(paymentId);
        if (payment.getStatus() == PaymentStatus.PENDING || payment.getStatus() == PaymentStatus.PROCESSING) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureCode("PROVIDER_CREATE_FAILED");
            payment.setFailureMessage(failureMessage);
        }
        return payment;
    }

    @Override
    @Transactional
    public WebhookMutation applyVerifiedWebhook(PaymentWebhookRequest request) {
        Payment payment = lockById(request.paymentId());
        validateWebhookReference(payment, request);
        boolean notifyPaid = false;
        boolean notifyFailed = false;
        if ("SUCCESS".equalsIgnoreCase(request.status())) {
            if (stateMachine.canApplySuccess(payment)) {
                payment.setStatus(PaymentStatus.PAID);
                payment.setPaidAt(Instant.now(clock));
                payment.setProviderTransactionId(request.providerTransactionId());
                ledger.record(new LedgerCommand(
                        "PLATFORM", null, payment.getOrderId(), payment.getId(),
                        LedgerEntryType.CUSTOMER_PAYMENT_CAPTURED, LedgerDirection.CREDIT,
                        payment.getAmount(), payment.getCurrency(),
                        "payment:" + payment.getId() + ":captured"));
            }
            notifyPaid = true;
        } else if ("FAILED".equalsIgnoreCase(request.status())) {
            if (stateMachine.canApplyFailure(payment)) {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureCode("PROVIDER_FAILED");
                payment.setFailureMessage("Payment provider reported failure");
                payment.setProviderTransactionId(request.providerTransactionId());
            }
            // A duplicate FAILED webhook is also a retry opportunity for the order callback.
            notifyFailed = payment.getStatus() == PaymentStatus.FAILED;
        } else {
            if (stateMachine.canApplyCancellation(payment)) {
                payment.setStatus(PaymentStatus.CANCELLED);
                payment.setCancelledAt(Instant.now(clock));
            }
        }
        return new WebhookMutation(payment, notifyPaid, notifyFailed);
    }

    @Override
    @Transactional
    public RefundPreparation prepareRefund(UUID orderId) {
        Payment payment = lock(orderId);
        if (payment.getMethod() == PaymentMethod.COD) {
            if (payment.getStatus() == PaymentStatus.CANCELLED) {
                cancelSnapshot(orderId);
                return new RefundPreparation(payment, false);
            }
            if (payment.getStatus() == PaymentStatus.COLLECTED) {
                throw conflict("A collected COD payment cannot be cancelled through refund");
            }
            payment.setStatus(PaymentStatus.CANCELLED);
            payment.setCancelledAt(Instant.now(clock));
            cancelSnapshot(orderId);
            return new RefundPreparation(payment, false);
        }
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            return new RefundPreparation(payment, false);
        }
        if (payment.getStatus() != PaymentStatus.PAID && payment.getStatus() != PaymentStatus.REFUND_PENDING) {
            throw conflict("Payment cannot be refunded in its current state");
        }
        payment.setStatus(PaymentStatus.REFUND_PENDING);
        return new RefundPreparation(payment, true);
    }

    @Override
    @Transactional
    public Payment completeRefund(UUID orderId) {
        Payment payment = lock(orderId);
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            return payment;
        }
        if (payment.getStatus() != PaymentStatus.REFUND_PENDING) {
            throw conflict("Payment is not waiting for a refund");
        }
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(Instant.now(clock));
        FinancialSnapshot snapshot = snapshot(orderId);
        ledger.record(new LedgerCommand(
                "PLATFORM", null, payment.getOrderId(), payment.getId(), LedgerEntryType.CUSTOMER_REFUND,
                LedgerDirection.DEBIT, payment.getAmount(), payment.getCurrency(),
                "refund:" + payment.getId() + ":customer"));
        reverseIfRecognized(payment, snapshot, LedgerEntryType.RESTAURANT_PAYABLE_CREATED,
                LedgerEntryType.RESTAURANT_PAYABLE_REVERSAL, snapshot.getRestaurantNetAmount(),
                "RESTAURANT", payment.getRestaurantId(), "restaurant");
        reverseIfRecognized(payment, snapshot, LedgerEntryType.DRIVER_PAYABLE_CREATED,
                LedgerEntryType.DRIVER_PAYABLE_REVERSAL, snapshot.getDriverNetAmount(),
                "DRIVER", payment.getDriverId(), "driver");
        reverseIfRecognized(payment, snapshot, LedgerEntryType.PLATFORM_REVENUE_RECOGNIZED,
                LedgerEntryType.PLATFORM_REVENUE_REVERSAL, snapshot.getPlatformRevenueAmount(),
                "PLATFORM", null, "platform");
        snapshot.setStatus(FinancialSnapshotStatus.REVERSED);
        return payment;
    }

    @Override
    @Transactional
    public Payment keepRefundPending(UUID orderId, String failureMessage) {
        Payment payment = lock(orderId);
        if (payment.getStatus() == PaymentStatus.REFUND_PENDING) {
            payment.setFailureCode("REFUND_PROVIDER_FAILED");
            payment.setFailureMessage(failureMessage);
        }
        return payment;
    }

    @Override
    @Transactional
    public Payment cancel(UUID orderId) {
        Payment payment = lock(orderId);
        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            cancelSnapshot(orderId);
            return payment;
        }
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            return payment;
        }
        if (payment.getStatus() == PaymentStatus.PAID || payment.getStatus() == PaymentStatus.REFUND_PENDING) {
            throw conflict("Paid payment must use the refund flow");
        }
        payment.setStatus(PaymentStatus.CANCELLED);
        payment.setCancelledAt(Instant.now(clock));
        cancelSnapshot(orderId);
        return payment;
    }

    @Override
    @Transactional(readOnly = true)
    public Payment findByOrder(UUID orderId) {
        return payments.findByOrderId(orderId).orElseThrow(() -> missing("Payment not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Payment findByOrderForOwner(UUID customerId, UUID orderId) {
        Payment payment = findByOrder(orderId);
        if (customerId == null || !customerId.equals(payment.getCustomerUserId())) {
            throw new PaymentException("PAYMENT_403", HttpStatus.FORBIDDEN, "Payment access denied");
        }
        return payment;
    }

    private void validateWebhookReference(Payment payment, PaymentWebhookRequest request) {
        if (!payment.getOrderId().equals(request.orderId())
                || payment.getAmount().compareTo(money(request.amount())) != 0
                || !payment.getCurrency().equalsIgnoreCase(request.currency())
                || (payment.getProviderTransactionId() != null
                && !payment.getProviderTransactionId().equals(request.providerTransactionId()))) {
            throw new PaymentException("PAYMENT_400", HttpStatus.BAD_REQUEST, "Webhook payment reference is invalid");
        }
    }

    private void reverseIfRecognized(Payment payment, FinancialSnapshot snapshot, LedgerEntryType originalType,
                                     LedgerEntryType reversalType, BigDecimal amount, String ownerType, UUID ownerId,
                                     String referencePart) {
        String originalReference = "settlement:" + payment.getId() + ":" + switch (originalType) {
            case RESTAURANT_PAYABLE_CREATED -> "restaurant-payable";
            case DRIVER_PAYABLE_CREATED -> "driver-payable";
            case PLATFORM_REVENUE_RECOGNIZED -> "platform-revenue";
            default -> throw new IllegalArgumentException("Unsupported reversal type: " + originalType);
        };
        if (ledgerEntries.findByIdempotencyReference(originalReference).isPresent()) {
            ledger.record(new LedgerCommand(ownerType, ownerId, payment.getOrderId(), payment.getId(), reversalType,
                    LedgerDirection.DEBIT, amount, snapshot.getCurrency(),
                    "refund:" + payment.getId() + ":" + referencePart));
        }
    }

    private Payment lock(UUID orderId) {
        Payment payment = payments.findByOrderId(orderId).orElseThrow(() -> missing("Payment not found"));
        return lockById(payment.getId());
    }

    private Payment lockById(UUID paymentId) {
        return payments.findWithLockById(paymentId).orElseThrow(() -> missing("Payment not found"));
    }

    private FinancialSnapshot snapshot(UUID orderId) {
        return snapshots.findByOrderId(orderId).orElseThrow(() -> missing("Financial snapshot not found"));
    }

    private void cancelSnapshot(UUID orderId) {
        FinancialSnapshot snapshot = snapshots.findByOrderId(orderId).orElse(null);
        if (snapshot != null && snapshot.getStatus() == FinancialSnapshotStatus.OPEN) {
            snapshot.setStatus(FinancialSnapshotStatus.CANCELLED);
        }
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private PaymentException missing(String message) {
        return new PaymentException("PAYMENT_404", HttpStatus.NOT_FOUND, message);
    }

    private PaymentException conflict(String message) {
        return new PaymentException("PAYMENT_409", HttpStatus.CONFLICT, message);
    }

    private PaymentException invalid(String message) {
        return new PaymentException("PAYMENT_400", HttpStatus.BAD_REQUEST, message);
    }
}
