package com.khanh.fooddelivery.payment_service.service.impl;

import com.khanh.fooddelivery.payment_service.client.OrderServiceClient;
import com.khanh.fooddelivery.payment_service.config.InternalApiProperties;
import com.khanh.fooddelivery.payment_service.dto.request.InternalCreatePaymentRequest;
import com.khanh.fooddelivery.payment_service.dto.request.PaymentWebhookRequest;
import com.khanh.fooddelivery.payment_service.dto.response.PaymentResponse;
import com.khanh.fooddelivery.payment_service.entity.FinancialSnapshot;
import com.khanh.fooddelivery.payment_service.entity.Payment;
import com.khanh.fooddelivery.payment_service.exception.PaymentException;
import com.khanh.fooddelivery.payment_service.mapper.PaymentMapper;
import com.khanh.fooddelivery.payment_service.model.PaymentMethod;
import com.khanh.fooddelivery.payment_service.model.PaymentStatus;
import com.khanh.fooddelivery.payment_service.provider.PaymentProviderGateway;
import com.khanh.fooddelivery.payment_service.provider.PaymentProviderResolver;
import com.khanh.fooddelivery.payment_service.repository.FinancialSnapshotRepository;
import com.khanh.fooddelivery.payment_service.service.FeePolicyService;
import com.khanh.fooddelivery.payment_service.service.FinancialBreakdown;
import com.khanh.fooddelivery.payment_service.service.FinancialCalculator;
import com.khanh.fooddelivery.payment_service.service.PaymentService;
import com.khanh.fooddelivery.payment_service.service.PaymentTransactionService;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentTransactionService transactions;
    private final FinancialSnapshotRepository snapshots;
    private final FeePolicyService feePolicies;
    private final FinancialCalculator calculator;
    private final PaymentMapper mapper;
    private final PaymentProviderResolver providers;
    private final OrderServiceClient orders;
    private final InternalApiProperties internalApi;
    private final Clock clock;

    @Override
    public PaymentResponse create(InternalCreatePaymentRequest request) {
        if (request == null || request.orderId() == null || request.customerUserId() == null) {
            throw invalid("Order and canonical customer IDs are required");
        }
        Payment existing = transactions.findExisting(request.orderId(), request.idempotencyKey()).orElse(null);
        if (existing != null) {
            if (existing.getMethod() == PaymentMethod.ONLINE
                    && (existing.getStatus() == PaymentStatus.PENDING
                    || existing.getStatus() == PaymentStatus.PROCESSING)
                    && (existing.getProviderTransactionId() == null || existing.getProviderTransactionId().isBlank())) {
                existing = attachProviderTransaction(existing);
            }
            return response(existing);
        }
        FinancialBreakdown breakdown = calculator.calculate(
                request, feePolicies.currentPolicy(Instant.now(clock)));
        Payment payment = transactions.createPending(request, breakdown);
        if (payment.getMethod() == PaymentMethod.ONLINE
                && (payment.getProviderTransactionId() == null || payment.getProviderTransactionId().isBlank())) {
            payment = attachProviderTransaction(payment);
        }
        return response(payment);
    }

    @Override
    public PaymentResponse byOrder(UUID orderId) {
        return response(transactions.findByOrder(orderId));
    }

    @Override
    public PaymentResponse byOrder(UUID customerId, UUID orderId) {
        return response(transactions.findByOrderForOwner(customerId, orderId));
    }

    @Override
    public PaymentResponse retry(UUID customerId, UUID orderId) {
        Payment payment = transactions.findByOrderForOwner(customerId, orderId);
        if (payment.getMethod() != PaymentMethod.ONLINE) {
            throw conflict("Only an online payment can be retried");
        }
        payment = transactions.beginRetry(payment.getId());
        PaymentProviderGateway.ProviderTransaction transaction;
        try {
            PaymentProviderGateway provider = providers.resolve(payment.getProvider());
            transaction = provider.create(payment);
        } catch (RuntimeException exception) {
            transactions.markRetryFailed(payment.getId(), exception.getMessage());
            throw providerUnavailable("Unable to create the payment retry", exception);
        }
        return response(transactions.attachProviderTransaction(payment.getId(), transaction));
    }

    @Override
    public PaymentResponse webhook(PaymentWebhookRequest request) {
        if (request == null || request.paymentId() == null || request.orderId() == null
                || request.providerTransactionId() == null || request.status() == null
                || request.amount() == null || request.currency() == null) {
            throw invalid("Webhook fields are required");
        }
        Payment payment = transactions.findByOrder(request.orderId());
        if (!payment.getId().equals(request.paymentId())) {
            throw invalid("Webhook payment reference is invalid");
        }
        if (payment.getMethod() != PaymentMethod.ONLINE) {
            throw invalid("COD payments do not accept provider webhooks");
        }
        if (!"SUCCESS".equalsIgnoreCase(request.status())
                && !"FAILED".equalsIgnoreCase(request.status())
                && !"CANCELLED".equalsIgnoreCase(request.status())) {
            throw invalid("Unsupported payment webhook status");
        }
        PaymentProviderGateway provider = providers.resolve(payment.getProvider());
        if (!provider.verifyWebhook(request.signature(), request.providerTransactionId(), request.status(),
                request.amount(), request.currency())) {
            throw invalid("Webhook verification failed");
        }

        PaymentTransactionService.WebhookMutation mutation = transactions.applyVerifiedWebhook(request);
        if (mutation.notifyOrderPaid()) {
            notifyOrderPaid(mutation.payment().getOrderId());
        } else if (mutation.notifyOrderFailed()) {
            notifyOrderFailed(mutation.payment().getOrderId());
        }
        return response(mutation.payment());
    }

    @Override
    public PaymentResponse refund(UUID orderId) {
        PaymentTransactionService.RefundPreparation preparation = transactions.prepareRefund(orderId);
        if (!preparation.providerCallRequired()) {
            return response(preparation.payment());
        }

        Payment payment = preparation.payment();
        PaymentProviderGateway provider = providers.resolve(payment.getProvider());
        try {
            provider.refund(payment);
        } catch (RuntimeException exception) {
            transactions.keepRefundPending(orderId, exception.getMessage());
            throw providerUnavailable("Unable to complete the payment refund; it remains retryable", exception);
        }
        return response(transactions.completeRefund(orderId));
    }

    @Override
    public PaymentResponse cancel(UUID orderId) {
        Payment payment = transactions.findByOrder(orderId);
        if (payment.getStatus() == PaymentStatus.PAID) {
            return refund(orderId);
        }
        return response(transactions.cancel(orderId));
    }

    private void notifyOrderPaid(UUID orderId) {
        try {
            var response = orders.paymentPaid(internalApi.getKey(), orderId);
            if (response == null || !response.success()) {
                throw new IllegalStateException("Order payment callback was rejected");
            }
        } catch (RuntimeException exception) {
            throw providerUnavailable("Payment was recorded but order confirmation is retryable", exception);
        }
    }

    private void notifyOrderFailed(UUID orderId) {
        try {
            var response = orders.paymentFailed(internalApi.getKey(), orderId);
            if (response == null || !response.success()) {
                throw new IllegalStateException("Order payment failure callback was rejected");
            }
        } catch (RuntimeException exception) {
            throw providerUnavailable("Payment failure was recorded but order notification is retryable", exception);
        }
    }

    private PaymentResponse response(Payment payment) {
        FinancialSnapshot snapshot = snapshots.findByOrderId(payment.getOrderId()).orElse(null);
        return mapper.toResponse(payment, snapshot);
    }

    private Payment attachProviderTransaction(Payment payment) {
        PaymentProviderGateway.ProviderTransaction transaction;
        try {
            PaymentProviderGateway provider = providers.resolve(payment.getProvider());
            transaction = provider.create(payment);
        } catch (RuntimeException exception) {
            transactions.markProviderCreateFailed(payment.getId(), exception.getMessage());
            throw providerUnavailable("Unable to create the payment provider transaction", exception);
        }
        return transactions.attachProviderTransaction(payment.getId(), transaction);
    }

    private PaymentException invalid(String message) {
        return new PaymentException("PAYMENT_400", HttpStatus.BAD_REQUEST, message);
    }

    private PaymentException conflict(String message) {
        return new PaymentException("PAYMENT_409", HttpStatus.CONFLICT, message);
    }

    private PaymentException providerUnavailable(String message, RuntimeException cause) {
        return new PaymentException("PAYMENT_503", HttpStatus.SERVICE_UNAVAILABLE,
                message + (cause.getMessage() == null ? "" : ": " + cause.getMessage()));
    }
}
