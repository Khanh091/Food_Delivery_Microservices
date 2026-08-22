package com.khanh.fooddelivery.payment_service.entity;

import com.khanh.fooddelivery.payment_service.model.SettlementBeneficiaryType;
import com.khanh.fooddelivery.payment_service.model.SettlementStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "settlements")
@Getter
@Setter
@NoArgsConstructor
public class Settlement {
    @Id
    private UUID id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SettlementBeneficiaryType beneficiaryType;
    @Column(nullable = false)
    private UUID beneficiaryId;
    @Column(nullable = false)
    private Instant periodFrom;
    @Column(nullable = false)
    private Instant periodTo;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal grossAmount;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal commissionAmount;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal adjustmentAmount;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal netAmount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SettlementStatus status;
    private Instant createdAt;
    private Instant finalizedAt;
    private Instant paidAt;
}
