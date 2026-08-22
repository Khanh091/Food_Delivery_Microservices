package com.khanh.fooddelivery.payment_service.provider;

import com.khanh.fooddelivery.payment_service.entity.Payment;
import com.khanh.fooddelivery.payment_service.model.PaymentProvider;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MockPaymentProvider implements PaymentProviderGateway {
    private final String secret;

    public MockPaymentProvider(@Value("${payment.mock.webhook-secret:dev-mock-secret}") String secret) {
        this.secret = secret;
    }

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.MOCK;
    }

    @Override
    public ProviderTransaction create(Payment payment) {
        // The payment id is the provider idempotency key. Retries must address the
        // same mock transaction instead of creating a second successful charge.
        String transaction = "MOCK-" + payment.getId();
        return new ProviderTransaction(transaction, "mock://payments/" + payment.getId());
    }

    @Override
    public boolean verifyWebhook(String signature, String providerTransactionId, String status,
                                 BigDecimal amount, String currency) {
        String canonical = providerTransactionId + ":" + status + ":" + amount + ":" + currency;
        byte[] digest;
        try {
            var md = MessageDigest.getInstance("SHA-256");
            digest = md.digest((secret + ":" + canonical).getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            return false;
        }
        StringBuilder expected = new StringBuilder();
        for (byte value : digest) expected.append(String.format("%02x", value));
        return MessageDigest.isEqual(expected.toString().getBytes(StandardCharsets.UTF_8),
                Objects.toString(signature, "").getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void refund(Payment payment) {
        // The mock provider settles immediately. Production providers implement the same port.
    }
}
