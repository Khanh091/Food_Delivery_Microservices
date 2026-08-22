package com.khanh.fooddelivery.payment_service.dto.request;

import com.khanh.fooddelivery.payment_service.model.SettlementBeneficiaryType;
import java.time.Instant;
import java.util.UUID;

public record SettlementRequest(SettlementBeneficiaryType beneficiaryType, UUID beneficiaryId,
                                Instant periodFrom, Instant periodTo) {
}
