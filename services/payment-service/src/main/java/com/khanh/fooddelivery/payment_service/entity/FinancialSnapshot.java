package com.khanh.fooddelivery.payment_service.entity;

import com.khanh.fooddelivery.payment_service.model.FinancialSnapshotStatus;
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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "financial_snapshots")
@Getter
@Setter
@NoArgsConstructor
public class FinancialSnapshot {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID orderId;

    private UUID restaurantId;

    private UUID driverId;

    @Column(nullable = false)
    private UUID feePolicyId;

    @Column(nullable = false)
    private Integer feePolicyVersion;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal foodGrossAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal deliveryGrossAmount;

    @Column(nullable = false, precision = 7, scale = 4)
    private BigDecimal restaurantCommissionRate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal restaurantCommissionAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal restaurantNetAmount;

    @Column(nullable = false, precision = 7, scale = 4)
    private BigDecimal driverCommissionRate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal driverCommissionAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal driverNetAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal platformRevenueAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal customerPayableAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal paymentProcessingFee;

    @Column(nullable = false, length = 12)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FinancialSnapshotStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
