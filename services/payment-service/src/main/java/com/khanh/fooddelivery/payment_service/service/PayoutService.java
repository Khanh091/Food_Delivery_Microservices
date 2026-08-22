package com.khanh.fooddelivery.payment_service.service;

import com.khanh.fooddelivery.payment_service.dto.response.PayoutResponse;
import java.util.UUID;

public interface PayoutService {
    PayoutResponse payout(UUID settlementId);
}
