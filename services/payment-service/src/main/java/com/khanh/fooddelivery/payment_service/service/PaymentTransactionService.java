package com.khanh.fooddelivery.payment_service.service;

import com.khanh.fooddelivery.payment_service.dto.request.InternalCreatePaymentRequest;
import com.khanh.fooddelivery.payment_service.dto.request.PaymentWebhookRequest;
import com.khanh.fooddelivery.payment_service.entity.FinancialSnapshot;
import com.khanh.fooddelivery.payment_service.entity.Payment;
import com.khanh.fooddelivery.payment_service.provider.PaymentProviderGateway;
import java.util.UUID;
import java.util.Optional;

/** Transactional persistence boundary. It never performs a network/provider call. */
public interface PaymentTransactionService {
    Optional<Payment> findExisting(UUID orderId, String idempotencyKey);

    Payment createPending(InternalCreatePaymentRequest request, FinancialBreakdown breakdown);

    Payment attachProviderTransaction(UUID paymentId, PaymentProviderGateway.ProviderTransaction transaction);

    Payment beginRetry(UUID paymentId);

    Payment markRetryFailed(UUID paymentId, String failureMessage);

    Payment markProviderCreateFailed(UUID paymentId, String failureMessage);

    Payment applyVerifiedWebhook(PaymentWebhookRequest request);

    RefundPreparation prepareRefund(UUID orderId);

    Payment completeRefund(UUID orderId);

    Payment keepRefundPending(UUID orderId, String failureMessage);

    Payment cancel(UUID orderId);

    Payment findByOrder(UUID orderId);

    Payment findByOrderForOwner(UUID customerId, UUID orderId);

    record RefundPreparation(Payment payment, boolean providerCallRequired) {
    }
}
