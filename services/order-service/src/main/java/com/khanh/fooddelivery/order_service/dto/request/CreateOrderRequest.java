package com.khanh.fooddelivery.order_service.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import com.khanh.fooddelivery.order_service.enums.PaymentMethod;

public record CreateOrderRequest(
        @NotNull UUID branchId,
        @Min(1) long cartVersion,
        @NotNull @Valid CheckoutDeliveryTargetRequest target,
        PaymentMethod paymentMethod
) {
    public CreateOrderRequest(UUID branchId, long cartVersion, CheckoutDeliveryTargetRequest target) {
        this(branchId, cartVersion, target, PaymentMethod.COD);
    }

    public PaymentMethod effectivePaymentMethod() {
        return paymentMethod == null ? PaymentMethod.COD : paymentMethod;
    }
}
