package com.khanh.fooddelivery.payment_service.provider;

import com.khanh.fooddelivery.payment_service.entity.Payment;
import com.khanh.fooddelivery.payment_service.model.PaymentProvider;

public interface PaymentProviderGateway {
    PaymentProvider provider();

    ProviderTransaction create(Payment payment);
    boolean verifyWebhook(String signature, String providerTransactionId, String status,
                          java.math.BigDecimal amount, String currency);
    void refund(Payment payment);

    record ProviderTransaction(String transactionId, String reference) {}
}
