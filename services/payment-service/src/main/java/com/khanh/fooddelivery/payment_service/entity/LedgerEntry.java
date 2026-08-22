package com.khanh.fooddelivery.payment_service.entity;

import com.khanh.fooddelivery.payment_service.model.LedgerDirection;
import com.khanh.fooddelivery.payment_service.model.LedgerEntryType;
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
@Table(name = "ledger_entries")
@Getter
@Setter
@NoArgsConstructor
public class LedgerEntry {
    @Id
    private UUID id;
    private String ownerType;
    private UUID ownerId;
    private UUID orderId;
    private UUID paymentId;
    private UUID settlementId;
    private UUID payoutId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 48)
    private LedgerEntryType entryType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private LedgerDirection direction;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 12)
    private String currency;

    @Column(nullable = false, unique = true, length = 200)
    private String idempotencyReference;

    @Column(nullable = false)
    private Instant occurredAt;

    @Column(nullable = false)
    private Instant createdAt;
}
