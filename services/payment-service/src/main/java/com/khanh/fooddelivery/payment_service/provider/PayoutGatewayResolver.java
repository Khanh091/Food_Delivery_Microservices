package com.khanh.fooddelivery.payment_service.provider;

import com.khanh.fooddelivery.payment_service.model.PayoutProvider;

public interface PayoutGatewayResolver {
    PayoutGateway resolve(PayoutProvider provider);
}
