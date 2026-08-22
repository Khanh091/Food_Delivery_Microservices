package com.khanh.fooddelivery.payment_service.provider;

import com.khanh.fooddelivery.payment_service.exception.PaymentException;
import com.khanh.fooddelivery.payment_service.model.PayoutProvider;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class PayoutGatewayResolverImpl implements PayoutGatewayResolver {
    private final Map<PayoutProvider, PayoutGateway> gateways;

    public PayoutGatewayResolverImpl(List<PayoutGateway> gateways) {
        EnumMap<PayoutProvider, PayoutGateway> resolved = new EnumMap<>(PayoutProvider.class);
        gateways.forEach(gateway -> resolved.put(gateway.provider(), gateway));
        this.gateways = Map.copyOf(resolved);
    }

    @Override
    public PayoutGateway resolve(PayoutProvider provider) {
        PayoutGateway gateway = gateways.get(provider);
        if (gateway == null) {
            throw new PaymentException("PAYMENT_PAYOUT_PROVIDER_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE,
                    "Payout provider is not configured: " + provider);
        }
        return gateway;
    }
}
