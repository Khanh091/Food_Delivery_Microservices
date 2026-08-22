package com.khanh.fooddelivery.payment_service.service;

import com.khanh.fooddelivery.payment_service.dto.response.FinancialFactsResponse;
import java.util.UUID;

public interface FinancialService {
    FinancialFactsResponse facts(UUID orderId);

    void completeDelivery(UUID orderId, UUID deliveryId, UUID driverId);
}
