package com.khanh.fooddelivery.restaurant_service.entity;

import com.khanh.fooddelivery.restaurant_service.common.entity.BaseAuditEntity;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantMemberRole;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantMemberStatus;
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
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "restaurant_members")
@Getter
@Setter
@NoArgsConstructor
public class RestaurantMember extends BaseAuditEntity {
    @Id private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private RestaurantBranch branch;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RestaurantMemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RestaurantMemberStatus status;

    private UUID invitedByUserId;
    private Instant joinedAt;
    @Version private long version;

    @PrePersist
    void id() {
        if (id == null) id = UUID.randomUUID();
    }
}
