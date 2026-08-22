package com.khanh.fooddelivery.payment_service.service;

import com.khanh.fooddelivery.payment_service.dto.response.FinancialFactsResponse;
import com.khanh.fooddelivery.payment_service.entity.FinancialSnapshot;
import com.khanh.fooddelivery.payment_service.entity.Payment;
import com.khanh.fooddelivery.payment_service.model.PaymentMethod;
import org.springframework.stereotype.Component;

@Component
public class FinancialFactsAssembler {
    public FinancialFactsResponse toResponse(Payment payment, FinancialSnapshot snapshot) {
        boolean cod = payment.getMethod() == PaymentMethod.COD;
        return new FinancialFactsResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getMethod(),
                payment.getStatus(),
                snapshot.getCurrency(),
                snapshot.getFoodGrossAmount(),
                snapshot.getDeliveryGrossAmount(),
                snapshot.getCustomerPayableAmount(),
                cod ? snapshot.getFoodGrossAmount() : zero(),
                cod ? snapshot.getCustomerPayableAmount() : zero(),
                snapshot.getDeliveryGrossAmount(),
                snapshot.getRestaurantCommissionAmount(),
                snapshot.getDriverCommissionAmount(),
                snapshot.getDriverNetAmount(),
                snapshot.getRestaurantNetAmount(),
                snapshot.getPlatformRevenueAmount(),
                snapshot.getFeePolicyId(),
                snapshot.getFeePolicyVersion(),
                payment.getRestaurantAdvanceConfirmedAt() != null,
                payment.getCashCollectedAt() != null);
    }

    private java.math.BigDecimal zero() {
        return java.math.BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
