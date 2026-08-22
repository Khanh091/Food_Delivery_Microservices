package com.khanh.fooddelivery.payment_service.entity;

import com.khanh.fooddelivery.payment_service.model.PayoutProvider;
import com.khanh.fooddelivery.payment_service.model.PayoutStatus;
import com.khanh.fooddelivery.payment_service.model.SettlementBeneficiaryType;
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
@Table(name = "payouts")
@Getter
@Setter
@NoArgsConstructor
public class Payout {
    @Id
    private UUID id;
    @Column(nullable = false)
    private UUID settlementId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SettlementBeneficiaryType beneficiaryType;
    @Column(nullable = false)
    private UUID beneficiaryId;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, length = 12)
    private String currency;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PayoutStatus status;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PayoutProvider provider;
    private String providerReference;
    private String failureReason;
    @Column(length = 2000)
    private String payoutDestinationSnapshot;
    private Instant createdAt;
    private Instant paidAt;
}
