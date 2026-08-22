package com.khanh.fooddelivery.payment_service.service;

import java.util.UUID;

public interface FinancialCompletionService {

    void completeDelivery(UUID orderId, UUID deliveryId, UUID driverId);
}
