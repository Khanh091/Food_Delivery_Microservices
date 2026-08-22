package com.khanh.fooddelivery.payment_service.service;

import com.khanh.fooddelivery.payment_service.dto.request.InternalCreatePaymentRequest;
import com.khanh.fooddelivery.payment_service.entity.FeePolicy;
import com.khanh.fooddelivery.payment_service.exception.PaymentException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Pure financial calculation. It has no persistence, clock or network dependency. */
@Component
public class FinancialCalculator {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int MONEY_SCALE = 2;
    private static final int RATE_SCALE = 4;

    public FinancialBreakdown calculate(InternalCreatePaymentRequest request, FeePolicy policy) {
        if (request == null || policy == null) {
            throw invalid("Payment amounts and fee policy are required");
        }

        BigDecimal food = money(request.foodGrossAmount());
        BigDecimal delivery = money(request.deliveryGrossAmount());
        BigDecimal discount = money(request.discountAmount());
        BigDecimal customerPayable = money(request.customerPayableAmount());
        if (food.signum() < 0 || delivery.signum() < 0 || discount.signum() < 0
                || customerPayable.signum() < 0) {
            throw invalid("Financial amounts cannot be negative");
        }

        BigDecimal expectedPayable = food.add(delivery).subtract(discount)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        if (expectedPayable.signum() < 0 || expectedPayable.compareTo(customerPayable) != 0) {
            throw invalid("Customer payable amount does not match the order totals");
        }

        BigDecimal restaurantRate = rate(policy.getRestaurantCommissionRate());
        BigDecimal driverRate = rate(policy.getDriverCommissionRate());
        BigDecimal restaurantCommission = percentage(food, restaurantRate);
        BigDecimal driverCommission = percentage(delivery, driverRate);
        String currency = blank(request.currency()) ? "VND" : request.currency().trim().toUpperCase(Locale.ROOT);
        if (!"VND".equals(currency)) {
            throw invalid("Only VND payments are supported");
        }
        return new FinancialBreakdown(
                policy.getId(),
                policy.getPolicyVersion(),
                food,
                delivery,
                restaurantRate,
                restaurantCommission,
                money(food.subtract(restaurantCommission)),
                driverRate,
                driverCommission,
                money(delivery.subtract(driverCommission)),
                money(restaurantCommission.add(driverCommission)),
                customerPayable,
                BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                currency);
    }

    private BigDecimal percentage(BigDecimal amount, BigDecimal rate) {
        return amount.multiply(rate).divide(HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal rate(BigDecimal value) {
        if (value == null || value.signum() < 0 || value.compareTo(HUNDRED) > 0) {
            throw invalid("Fee policy rates must be between 0 and 100 percent");
        }
        return value.setScale(RATE_SCALE, RoundingMode.HALF_UP);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private PaymentException invalid(String message) {
        return new PaymentException("PAYMENT_400", HttpStatus.BAD_REQUEST, message);
    }
}
