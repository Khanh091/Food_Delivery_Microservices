package com.khanh.fooddelivery.payment_service.provider;

import com.khanh.fooddelivery.payment_service.entity.Payout;
import com.khanh.fooddelivery.payment_service.model.PayoutProvider;
import org.springframework.stereotype.Component;

@Component
public class MockPayoutGateway implements PayoutGateway {
    @Override
    public PayoutProvider provider() {
        return PayoutProvider.MOCK;
    }

    @Override
    public String submit(Payout payout) {
        return "MOCK-PAYOUT-" + payout.getId();
    }
}
