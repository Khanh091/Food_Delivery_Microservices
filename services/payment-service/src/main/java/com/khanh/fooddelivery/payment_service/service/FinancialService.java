package com.khanh.fooddelivery.payment_service.service;

import com.khanh.fooddelivery.payment_service.dto.response.FinancialFactsResponse;
import com.khanh.fooddelivery.payment_service.model.PaymentMethod;
import java.util.UUID;

public interface FinancialService {
    FinancialFactsResponse facts(UUID orderId);

    PaymentMethod completeDelivery(UUID orderId, UUID deliveryId, UUID driverId);
}
