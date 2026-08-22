package com.khanh.fooddelivery.order_service.client.dto.request;

import com.khanh.fooddelivery.order_service.enums.PaymentMethod;
import java.math.BigDecimal;
import java.util.UUID;

public record InternalCreatePaymentRequest(UUID orderId, UUID customerUserId, UUID restaurantId, UUID branchId,
                                           PaymentMethod method, BigDecimal foodGrossAmount,
                                           BigDecimal deliveryGrossAmount, BigDecimal discountAmount,
                                           BigDecimal customerPayableAmount, String currency,
                                           String idempotencyKey) {
}
