package com.khanh.fooddelivery.notification_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
        name = "notification_deliveries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notification_deliveries_source_event",
                columnNames = "source_event_id"
        )
)
@Getter
@Setter
@NoArgsConstructor
public class NotificationDelivery {

    @Id
    private UUID id;

    @Version
    private Long version;

    @Column(name = "source_event_id", nullable = false)
    private UUID sourceEventId;

    @Column(nullable = false)
    private UUID offerId;

    @Column(nullable = false)
    private UUID deliveryId;

    @Column(nullable = false)
    private UUID driverId;

    @Column(nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NotificationDeliveryStatus status;

    @Column(nullable = false)
    private int attemptCount;

    @Column(length = 1000)
    private String lastError;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
