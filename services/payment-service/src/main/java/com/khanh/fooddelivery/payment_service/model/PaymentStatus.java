package com.khanh.fooddelivery.payment_service.model;

public enum PaymentStatus {
    PENDING,
    PROCESSING,
    PAID,
    FAILED,
    CANCELLED,
    COLLECTED,
    REFUND_PENDING,
    REFUNDED
}
