package com.khanh.fooddelivery.payment_service.service;

import com.khanh.fooddelivery.payment_service.dto.request.SettlementRequest;
import com.khanh.fooddelivery.payment_service.dto.response.SettlementResponse;

public interface SettlementService {
    SettlementResponse create(SettlementRequest request);
}
