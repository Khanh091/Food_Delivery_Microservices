package com.khanh.fooddelivery.payment_service.entity;

import com.khanh.fooddelivery.payment_service.model.PaymentMethod;
import com.khanh.fooddelivery.payment_service.model.PaymentProvider;
import com.khanh.fooddelivery.payment_service.model.PaymentStatus;
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
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
public class Payment {
    @Id
    private UUID id;

    @Version
    private Long version;

    @Column(nullable = false, unique = true)
    private UUID orderId;

    @Column(length = 200)
    private String idempotencyKey;

    @Column(nullable = false)
    private UUID customerUserId;

    private UUID restaurantId;

    private UUID deliveryId;

    private UUID driverId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PaymentStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 12)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PaymentProvider provider;

    private String providerTransactionId;
    private String providerReference;
    private String failureCode;

    @Column(length = 500)
    private String failureMessage;

    private Instant paidAt;
    private Instant collectedAt;
    private Instant cancelledAt;
    private Instant refundedAt;
    private Instant restaurantAdvanceConfirmedAt;
    private Instant cashCollectedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
