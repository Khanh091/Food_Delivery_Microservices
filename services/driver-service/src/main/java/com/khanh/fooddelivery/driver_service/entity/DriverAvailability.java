package com.khanh.fooddelivery.driver_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "driver_availability")
@Getter
@Setter
@NoArgsConstructor
public class DriverAvailability {

    @Id
    private UUID id;

    @Version
    private Long version;

    @Column(nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false)
    private boolean available;

    private UUID activeDeliveryId;

    private UUID pendingOfferDeliveryId;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}