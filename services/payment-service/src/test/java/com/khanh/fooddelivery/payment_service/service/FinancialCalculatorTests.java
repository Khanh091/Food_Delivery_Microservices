package com.khanh.fooddelivery.payment_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.khanh.fooddelivery.payment_service.dto.request.InternalCreatePaymentRequest;
import com.khanh.fooddelivery.payment_service.entity.FeePolicy;
import com.khanh.fooddelivery.payment_service.exception.PaymentException;
import com.khanh.fooddelivery.payment_service.model.FeePolicyStatus;
import com.khanh.fooddelivery.payment_service.model.PaymentMethod;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FinancialCalculatorTests {
    private final FinancialCalculator calculator = new FinancialCalculator();

    @Test
    void calculatesRequiredBreakdownWithoutFloatingPointDrift() {
        FinancialBreakdown result = calculator.calculate(request("50000", "25000", "0", "75000"), policy("30", "30"));

        assertThat(result.customerPayableAmount()).isEqualByComparingTo("75000.00");
        assertThat(result.restaurantCommissionAmount()).isEqualByComparingTo("15000.00");
        assertThat(result.restaurantNetAmount()).isEqualByComparingTo("35000.00");
        assertThat(result.driverCommissionAmount()).isEqualByComparingTo("7500.00");
        assertThat(result.driverNetAmount()).isEqualByComparingTo("17500.00");
        assertThat(result.platformRevenueAmount()).isEqualByComparingTo("22500.00");
    }

    @Test
    void validatesCustomerPayableAgainstAuthoritativeOrderTotals() {
        assertThatThrownBy(() -> calculator.calculate(request("50000", "25000", "1000", "75000"), policy("30", "30")))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    void rejectsNegativeAmountsAndInvalidRates() {
        assertThatThrownBy(() -> calculator.calculate(request("-1", "25000", "0", "24999"), policy("30", "30")))
                .isInstanceOf(PaymentException.class);
        assertThatThrownBy(() -> calculator.calculate(request("50000", "25000", "0", "75000"), policy("101", "30")))
                .isInstanceOf(PaymentException.class);
    }

    private InternalCreatePaymentRequest request(String food, String delivery, String discount, String payable) {
        return new InternalCreatePaymentRequest(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                PaymentMethod.COD, new BigDecimal(food), new BigDecimal(delivery), new BigDecimal(discount),
                new BigDecimal(payable), "VND", "idempotency");
    }

    private FeePolicy policy(String restaurant, String driver) {
        FeePolicy policy = new FeePolicy();
        policy.setId(UUID.randomUUID());
        policy.setPolicyVersion(1);
        policy.setRestaurantCommissionRate(new BigDecimal(restaurant));
        policy.setDriverCommissionRate(new BigDecimal(driver));
        policy.setEffectiveFrom(Instant.parse("2026-01-01T00:00:00Z"));
        policy.setStatus(FeePolicyStatus.ACTIVE);
        return policy;
    }
}
