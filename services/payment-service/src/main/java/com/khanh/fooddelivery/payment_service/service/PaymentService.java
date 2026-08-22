package com.khanh.fooddelivery.payment_service.service;

import com.khanh.fooddelivery.payment_service.dto.request.InternalCreatePaymentRequest;
import com.khanh.fooddelivery.payment_service.dto.request.PaymentWebhookRequest;
import com.khanh.fooddelivery.payment_service.dto.response.PaymentResponse;
import java.util.UUID;

public interface PaymentService {
    PaymentResponse create(InternalCreatePaymentRequest request);

    PaymentResponse byOrder(UUID orderId);

    PaymentResponse byOrder(UUID customerId, UUID orderId);

    PaymentResponse retry(UUID customerId, UUID orderId);

    PaymentResponse webhook(PaymentWebhookRequest request);

    PaymentResponse refund(UUID orderId);

    PaymentResponse cancel(UUID orderId);
}
