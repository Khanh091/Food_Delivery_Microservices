package com.khanh.fooddelivery.delivery_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "delivery_offers")
@Getter
@Setter
@NoArgsConstructor
public class DeliveryOffer {

    @Id
    private UUID id;

    @Version
    private Long version;

    @Column(nullable = false)
    private UUID deliveryId;

    @Column(nullable = false)
    private UUID driverId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryOfferStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant offeredAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant respondedAt;
}
