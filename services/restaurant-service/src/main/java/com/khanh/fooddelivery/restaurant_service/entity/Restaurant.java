package com.khanh.fooddelivery.restaurant_service.entity;

import com.khanh.fooddelivery.restaurant_service.common.entity.BaseAuditEntity;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantStatus;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantVerificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "restaurants")
@Getter
@Setter
@NoArgsConstructor
public class Restaurant extends BaseAuditEntity {
    @Id private UUID id;

    @Column(nullable = false)
    private UUID ownerUserId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_application_id", unique = true)
    private RestaurantPartnerApplication partnerApplication;

    @Column(nullable = false, unique = true)
    private String restaurantCode;

    @Column(nullable = false)
    private String name;

    private String legalName;

    @Column(columnDefinition = "text")
    private String description;

    private String logoUrl;
    private String coverImageUrl;
    private String phoneNumber;
    private String email;
    private String taxCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RestaurantStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RestaurantVerificationStatus verificationStatus;

    @Column(nullable = false)
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(nullable = false)
    private long totalReviews;

    @Version private long version;

    @PrePersist
    void id() {
        if (id == null) id = UUID.randomUUID();
    }
}
