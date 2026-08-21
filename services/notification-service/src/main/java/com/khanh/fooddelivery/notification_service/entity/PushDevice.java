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
@Table(name = "push_devices", uniqueConstraints = @UniqueConstraint(name = "uk_push_devices_token", columnNames = "expo_push_token"))
@Getter
@Setter
@NoArgsConstructor
public class PushDevice {

    @Id
    private UUID id;

    @Version
    private Long version;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID driverId;

    @Column(name = "expo_push_token", nullable = false, length = 255)
    private String expoPushToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PushPlatform platform;

    @Column(length = 255)
    private String deviceId;

    @Column(nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
