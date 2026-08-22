package com.khanh.fooddelivery.payment_service.event;

import java.time.Instant;
import java.util.UUID;

public record PaymentLifecycleEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        UUID aggregateId,
        int version,
        Payload payload
) {

    public static final String PAYMENT_SUCCEEDED = "PAYMENT_SUCCEEDED";
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String PAYMENT_COLLECTED = "PAYMENT_COLLECTED";
    public static final int VERSION = 1;

    public record Payload(
            UUID paymentId,
            UUID orderId,
            String paymentStatus
    ) {
    }
}
