package com.khanh.fooddelivery.payment_service.service.impl;

import com.khanh.fooddelivery.payment_service.service.FinancialCompletionService;
import com.khanh.fooddelivery.payment_service.service.FinancialService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FinancialCompletionServiceImpl implements FinancialCompletionService {

    private final FinancialService financials;

    @Override
    public void completeDelivery(UUID orderId, UUID deliveryId, UUID driverId) {
        financials.completeDelivery(orderId, deliveryId, driverId);
    }
}
