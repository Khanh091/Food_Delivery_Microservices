package com.khanh.fooddelivery.payment_service.service.impl;

import com.khanh.fooddelivery.payment_service.dto.response.PayoutResponse;
import com.khanh.fooddelivery.payment_service.entity.Payout;
import com.khanh.fooddelivery.payment_service.mapper.PayoutMapper;
import com.khanh.fooddelivery.payment_service.provider.PayoutGateway;
import com.khanh.fooddelivery.payment_service.provider.PayoutGatewayResolver;
import com.khanh.fooddelivery.payment_service.service.PayoutService;
import com.khanh.fooddelivery.payment_service.service.PayoutTransactionService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.khanh.fooddelivery.payment_service.exception.PaymentException;

@Service
@RequiredArgsConstructor
public class PayoutServiceImpl implements PayoutService {
    private final PayoutTransactionService transactions;
    private final PayoutGatewayResolver gateways;
    private final PayoutMapper mapper;

    @Override
    public PayoutResponse payout(UUID settlementId) {
        Payout payout = transactions.prepare(settlementId);
        if (payout.getStatus() == com.khanh.fooddelivery.payment_service.model.PayoutStatus.PAID) {
            return mapper.toResponse(payout);
        }
        try {
            // A persisted provider reference means the external transfer was already
            // submitted. Finalize locally instead of submitting the same payout again.
            if (payout.getProviderReference() == null || payout.getProviderReference().isBlank()) {
                PayoutGateway gateway = gateways.resolve(payout.getProvider());
                String reference = gateway.submit(payout);
                payout = transactions.markSubmitted(payout.getId(), reference);
            }
            return mapper.toResponse(transactions.complete(payout.getId()));
        } catch (RuntimeException exception) {
            // A provider failure before a reference is persisted is retryable. If
            // finalization fails after a reference exists, the payout stays PROCESSING
            // and the next attempt only completes the local state.
            if (payout.getProviderReference() == null || payout.getProviderReference().isBlank()) {
                transactions.markFailed(payout.getId(), exception.getMessage());
            }
            throw new PaymentException("PAYMENT_PAYOUT_FAILED", HttpStatus.SERVICE_UNAVAILABLE,
                    "Payout remains retryable: " + (exception.getMessage() == null ? "provider failure" : exception.getMessage()));
        }
    }
}
