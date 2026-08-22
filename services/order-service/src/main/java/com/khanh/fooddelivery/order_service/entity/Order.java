package com.khanh.fooddelivery.order_service.entity;

import com.khanh.fooddelivery.order_service.enums.OrderStatus;
import com.khanh.fooddelivery.order_service.enums.PaymentMethod;
import com.khanh.fooddelivery.order_service.enums.PaymentStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "customer_orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    private UUID id;

    @Version
    private Long version;

    @Column(nullable = false)
    private UUID customerId;

    @Column(nullable = false)
    private UUID restaurantId;

    @Column(nullable = false)
    private UUID branchId;

    @Column(nullable = false, unique = true)
    private String orderCode;

    @Column(nullable = false)
    private String restaurantName;

    @Column(nullable = false)
    private String branchName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PaymentStatus paymentStatus;

    private UUID paymentId;

    private UUID feePolicyId;

    private Integer feePolicyVersion;

    @Column(precision = 19, scale = 2)
    private BigDecimal restaurantCommissionAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal restaurantNetAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal driverCommissionAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal driverNetAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal platformRevenueAmount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private BigDecimal itemsSubtotal;

    @Column(nullable = false)
    private BigDecimal deliveryFee;

    @Column(nullable = false)
    private BigDecimal discountAmount;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private String addressDisplayLabel;

    @Column(nullable = false)
    private String recipientName;

    @Column(nullable = false)
    private String recipientPhone;

    @Column(nullable = false, length = 1000)
    private String addressLine;

    @Column(length = 1500)
    private String formattedAddress;

    private String ward;

    private String district;

    private String city;

    private BigDecimal latitude;

    private BigDecimal longitude;

    @Column(length = 500)
    private String rejectionReason;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("sortOrder ASC")
    private List<OrderItem> items = new ArrayList<>();
}
