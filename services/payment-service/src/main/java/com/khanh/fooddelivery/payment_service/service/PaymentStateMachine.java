package com.khanh.fooddelivery.payment_service.service;

import com.khanh.fooddelivery.payment_service.entity.Payment;
import com.khanh.fooddelivery.payment_service.exception.PaymentException;
import com.khanh.fooddelivery.payment_service.model.PaymentMethod;
import com.khanh.fooddelivery.payment_service.model.PaymentStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class PaymentStateMachine {
    public void ensureOnlineRetryable(Payment payment) {
        if (payment.getMethod() != PaymentMethod.ONLINE || payment.getStatus() != PaymentStatus.FAILED) {
            throw conflict("Only a failed online payment can be retried");
        }
    }

    public boolean canApplySuccess(Payment payment) {
        if (payment.getStatus() == PaymentStatus.PAID) {
            return false;
        }
        if (payment.getStatus() == PaymentStatus.CANCELLED
                || payment.getStatus() == PaymentStatus.COLLECTED
                || payment.getStatus() == PaymentStatus.REFUND_PENDING
                || payment.getStatus() == PaymentStatus.REFUNDED) {
            throw conflict("Payment cannot be marked paid from " + payment.getStatus());
        }
        return true;
    }

    public boolean canApplyFailure(Payment payment) {
        if (payment.getStatus() == PaymentStatus.PAID
                || payment.getStatus() == PaymentStatus.COLLECTED
                || payment.getStatus() == PaymentStatus.REFUND_PENDING
                || payment.getStatus() == PaymentStatus.REFUNDED
                || payment.getStatus() == PaymentStatus.CANCELLED) {
            return false;
        }
        return payment.getStatus() != PaymentStatus.FAILED;
    }

    public boolean canApplyCancellation(Payment payment) {
        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            return false;
        }
        if (payment.getStatus() == PaymentStatus.PAID
                || payment.getStatus() == PaymentStatus.COLLECTED
                || payment.getStatus() == PaymentStatus.REFUND_PENDING
                || payment.getStatus() == PaymentStatus.REFUNDED) {
            throw conflict("Payment cannot be cancelled from " + payment.getStatus());
        }
        return true;
    }

    public void ensureCod(Payment payment, String action) {
        if (payment.getMethod() != PaymentMethod.COD) {
            throw conflict(action + " is only available for COD payments");
        }
    }

    public void ensureActiveForCash(Payment payment) {
        if (payment.getStatus() == PaymentStatus.CANCELLED || payment.getStatus() == PaymentStatus.REFUNDED
                || payment.getStatus() == PaymentStatus.REFUND_PENDING) {
            throw conflict("Payment is no longer active");
        }
    }

    public void ensureOnlinePaid(Payment payment) {
        if (payment.getStatus() != PaymentStatus.PAID) {
            throw conflict("Online payment is not paid");
        }
    }

    private PaymentException conflict(String message) {
        return new PaymentException("PAYMENT_409", HttpStatus.CONFLICT, message);
    }
}
