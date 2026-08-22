package com.khanh.fooddelivery.payment_service.entity;

import com.khanh.fooddelivery.payment_service.model.FeePolicyStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "fee_policies")
@Getter
@Setter
@NoArgsConstructor
public class FeePolicy {
    @Id
    private UUID id;

    @Version
    private Long version;

    @Column(nullable = false, unique = true)
    private Integer policyVersion;

    @Column(nullable = false, precision = 7, scale = 4)
    private BigDecimal restaurantCommissionRate;

    @Column(nullable = false, precision = 7, scale = 4)
    private BigDecimal driverCommissionRate;

    @Column(nullable = false)
    private Instant effectiveFrom;

    private Instant effectiveTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FeePolicyStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
