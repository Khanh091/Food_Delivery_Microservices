package com.khanh.fooddelivery.payment_service.provider;

import com.khanh.fooddelivery.payment_service.model.PaymentProvider;

public interface PaymentProviderResolver {
    PaymentProviderGateway resolve(PaymentProvider provider);
}
