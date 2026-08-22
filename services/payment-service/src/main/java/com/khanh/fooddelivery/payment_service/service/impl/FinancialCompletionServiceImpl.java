package com.khanh.fooddelivery.payment_service.service.impl;

import com.khanh.fooddelivery.payment_service.client.OrderServiceClient;
import com.khanh.fooddelivery.payment_service.config.InternalApiProperties;
import com.khanh.fooddelivery.payment_service.service.FinancialCompletionService;
import com.khanh.fooddelivery.payment_service.service.FinancialService;
import com.khanh.fooddelivery.payment_service.model.PaymentMethod;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Runs the financial transaction first, then updates the order's payment
 * projection after the financial transaction has committed.
 */
@Service
@RequiredArgsConstructor
public class FinancialCompletionServiceImpl implements FinancialCompletionService {

    private final FinancialService financials;
    private final OrderServiceClient orders;
    private final InternalApiProperties internalApi;

    @Override
    public void completeDelivery(UUID orderId, UUID deliveryId, UUID driverId) {
        PaymentMethod method = financials.completeDelivery(orderId, deliveryId, driverId);
        if (method == PaymentMethod.COD) {
            var response = orders.paymentCollected(internalApi.getKey(), orderId);
            if (response == null || !response.success()) {
                throw new IllegalStateException("Order payment projection callback was rejected");
            }
        }
    }
}
