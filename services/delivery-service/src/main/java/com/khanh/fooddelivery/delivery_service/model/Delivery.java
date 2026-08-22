package com.khanh.fooddelivery.delivery_service.model;

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
@Table(name = "deliveries")
@Getter
@Setter
@NoArgsConstructor
public class Delivery {

    @Id
    private UUID id;

    @Version
    private Long version;

    @Column(nullable = false, unique = true)
    private UUID orderId;

    @Column(nullable = false)
    private UUID restaurantId;

    @Column(nullable = false)
    private UUID branchId;

    @Column(nullable = false)
    private UUID customerId;

    private UUID driverId;

    private BigDecimal pickupLatitude;

    private BigDecimal pickupLongitude;

    @Column(length = 1500)
    private String pickupAddress;

    @Column(length = 500)
    private String customerAddressLabel;

    private BigDecimal customerLatitude;

    private BigDecimal customerLongitude;

    @Column(length = 16)
    private String paymentMethod;

    @Column(precision = 19, scale = 2)
    private BigDecimal requiredRestaurantAdvance;

    @Column(precision = 19, scale = 2)
    private BigDecimal customerCashToCollect;

    @Column(precision = 19, scale = 2)
    private BigDecimal driverGrossEarning;

    @Column(precision = 19, scale = 2)
    private BigDecimal restaurantCommissionAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal driverCommissionAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal driverNetEarning;

    @Column(precision = 19, scale = 2)
    private BigDecimal restaurantNetAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal platformRevenueAmount;

    @Column(nullable = false)
    private boolean restaurantAdvanceConfirmed;

    @Column(nullable = false)
    private boolean customerCashCollected;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status;

    @Column(nullable = false)
    private String restaurantName;

    @Column(nullable = false)
    private String branchName;

    @Column(nullable = false, length = 1000)
    private String customerAddress;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
