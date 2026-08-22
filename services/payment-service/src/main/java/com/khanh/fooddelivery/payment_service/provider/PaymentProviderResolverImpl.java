package com.khanh.fooddelivery.payment_service.provider;

import com.khanh.fooddelivery.payment_service.exception.PaymentException;
import com.khanh.fooddelivery.payment_service.model.PaymentProvider;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class PaymentProviderResolverImpl implements PaymentProviderResolver {
    private final Map<PaymentProvider, PaymentProviderGateway> providers;

    public PaymentProviderResolverImpl(List<PaymentProviderGateway> gateways) {
        EnumMap<PaymentProvider, PaymentProviderGateway> resolved = new EnumMap<>(PaymentProvider.class);
        gateways.forEach(gateway -> resolved.put(gateway.provider(), gateway));
        this.providers = Map.copyOf(resolved);
    }

    @Override
    public PaymentProviderGateway resolve(PaymentProvider provider) {
        PaymentProviderGateway gateway = providers.get(provider);
        if (gateway == null) {
            throw new PaymentException("PAYMENT_PROVIDER_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE,
                    "Payment provider is not configured: " + provider);
        }
        return gateway;
    }
}
