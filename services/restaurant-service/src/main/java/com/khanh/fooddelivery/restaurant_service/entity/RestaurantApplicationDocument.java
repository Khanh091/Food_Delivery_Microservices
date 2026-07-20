package com.khanh.fooddelivery.restaurant_service.entity;

import com.khanh.fooddelivery.restaurant_service.common.entity.BaseAuditEntity;
import com.khanh.fooddelivery.restaurant_service.enums.ApplicationDocumentType;
import com.khanh.fooddelivery.restaurant_service.enums.DocumentVerificationStatus;
import com.khanh.fooddelivery.restaurant_service.storage.StorageProvider;
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
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "restaurant_application_documents")
@Getter
@Setter
@NoArgsConstructor
public class RestaurantApplicationDocument extends BaseAuditEntity {
    @Id private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id")
    private RestaurantPartnerApplication application;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationDocumentType documentType;

    private String documentNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_provider", nullable = false, length = 30)
    private StorageProvider storageProvider;

    @Column(nullable = false, length = 500)
    private String storageKey;

    @Column(nullable = false, length = 1000)
    private String fileUrl;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 100)
    private String mimeType;

    @Column(nullable = false)
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentVerificationStatus verificationStatus;

    private String rejectionReason;
    private LocalDate issuedAt;
    private LocalDate expiresAt;
    private Instant verifiedAt;
    private UUID verifiedByUserId;
    @Version private long version;

    @PrePersist
    void id() {
        if (id == null) id = UUID.randomUUID();
    }
}
