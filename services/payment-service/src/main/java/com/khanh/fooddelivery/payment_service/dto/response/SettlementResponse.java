package com.khanh.fooddelivery.payment_service.dto.response;

import com.khanh.fooddelivery.payment_service.model.SettlementBeneficiaryType;
import com.khanh.fooddelivery.payment_service.model.SettlementStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SettlementResponse(UUID id, SettlementBeneficiaryType beneficiaryType, UUID beneficiaryId,
                                 Instant periodFrom, Instant periodTo, BigDecimal grossAmount,
                                 BigDecimal commissionAmount, BigDecimal adjustmentAmount,
                                 BigDecimal netAmount, SettlementStatus status, Instant paidAt) {
}
