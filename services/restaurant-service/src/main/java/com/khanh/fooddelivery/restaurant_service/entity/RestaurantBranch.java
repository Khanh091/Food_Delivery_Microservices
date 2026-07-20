package com.khanh.fooddelivery.restaurant_service.entity;

import com.khanh.fooddelivery.restaurant_service.common.entity.BaseAuditEntity;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantBranchStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "restaurant_branches")
@Getter
@Setter
@NoArgsConstructor
public class RestaurantBranch extends BaseAuditEntity {
    @Id private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;

    @Column(nullable = false)
    private String branchCode;

    @Column(nullable = false)
    private String name;

    private String phoneNumber;
    private String email;

    @Column(nullable = false)
    private String addressLine;

    private String ward;
    private String district;
    private String city;

    @Column(nullable = false)
    private BigDecimal latitude;

    @Column(nullable = false)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RestaurantBranchStatus status;

    @Column(nullable = false)
    private boolean acceptingOrders;

    @Column(nullable = false)
    private BigDecimal minimumOrderAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer defaultPreparationMinutes = 20;

    @Version private long version;

    @PrePersist
    void id() {
        if (id == null) id = UUID.randomUUID();
    }
}
