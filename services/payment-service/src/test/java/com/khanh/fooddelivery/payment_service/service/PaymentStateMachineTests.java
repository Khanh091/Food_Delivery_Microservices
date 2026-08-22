package com.khanh.fooddelivery.payment_service.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.khanh.fooddelivery.payment_service.entity.Payment;
import com.khanh.fooddelivery.payment_service.model.PaymentMethod;
import com.khanh.fooddelivery.payment_service.model.PaymentStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentStateMachineTests {
    private final PaymentStateMachine stateMachine = new PaymentStateMachine();

    @Test
    void refundedPaymentCannotBecomePaid() {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setMethod(PaymentMethod.ONLINE);
        payment.setStatus(PaymentStatus.REFUNDED);

        assertThatThrownBy(() -> stateMachine.canApplySuccess(payment))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("cannot be marked paid");
    }

    @Test
    void paidPaymentCannotBeRetried() {
        Payment payment = new Payment();
        payment.setMethod(PaymentMethod.ONLINE);
        payment.setStatus(PaymentStatus.PAID);

        assertThatThrownBy(() -> stateMachine.ensureOnlineRetryable(payment))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("can be retried");
    }
}
