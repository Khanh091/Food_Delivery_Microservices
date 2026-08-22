package com.khanh.fooddelivery.payment_service.provider;

import com.khanh.fooddelivery.payment_service.entity.Payout;
import com.khanh.fooddelivery.payment_service.model.PayoutProvider;

public interface PayoutGateway {
    PayoutProvider provider();

    String submit(Payout payout);
}
