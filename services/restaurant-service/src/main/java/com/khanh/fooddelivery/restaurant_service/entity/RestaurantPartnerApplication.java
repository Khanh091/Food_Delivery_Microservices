package com.khanh.fooddelivery.restaurant_service.entity;

import com.khanh.fooddelivery.restaurant_service.common.entity.BaseAuditEntity;
import com.khanh.fooddelivery.restaurant_service.enums.BusinessType;
import com.khanh.fooddelivery.restaurant_service.enums.PartnerApplicationStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "restaurant_partner_applications")
@Getter
@Setter
@NoArgsConstructor
public class RestaurantPartnerApplication extends BaseAuditEntity {
    @Id private UUID id;

    @Column(nullable = false)
    private UUID applicantUserId;

    @Column(nullable = false)
    private String businessName;

    @Enumerated(EnumType.STRING)
    private BusinessType businessType;

    private String taxCode;

    @Column(nullable = false)
    private String representativeName;

    @Column(nullable = false)
    private String representativePhone;

    private String representativeEmail;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private String city;

    private String district;

    @Column(nullable = false)
    private String businessAddress;

    @Column(nullable = false)
    private Integer expectedBranchCount = 1;

    private Integer estimatedDailyOrders;
    private String mainCuisine;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartnerApplicationStatus status;

    private Instant submittedAt;
    private Instant reviewedAt;
    private UUID reviewedByUserId;
    private String rejectionReason;
    @Version private long version;

    @OneToMany(
            mappedBy = "application",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<RestaurantApplicationDocument> documents = new ArrayList<>();

    @PrePersist
    void id() {
        if (id == null) id = UUID.randomUUID();
    }
}
