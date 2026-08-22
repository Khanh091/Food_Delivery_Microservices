package com.khanh.fooddelivery.payment_service.service;

import com.khanh.fooddelivery.payment_service.dto.request.CashActionRequest;

public interface CodPaymentService {
    void confirmRestaurantAdvance(CashActionRequest request);

    void collectCash(CashActionRequest request);
}
