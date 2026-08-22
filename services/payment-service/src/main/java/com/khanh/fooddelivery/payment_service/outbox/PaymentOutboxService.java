package com.khanh.fooddelivery.payment_service.outbox;

import com.khanh.fooddelivery.payment_service.entity.Payment;

public interface PaymentOutboxService {
    void publishPaymentSucceeded(Payment payment);

    void publishPaymentFailed(Payment payment);

    void publishPaymentCollected(Payment payment);
}
