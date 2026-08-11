package com.khanh.fooddelivery.catalog_service.entity;

import com.khanh.fooddelivery.catalog_service.common.entity.BaseAuditEntity;
import com.khanh.fooddelivery.catalog_service.enums.CatalogStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "menus")
@Getter
@Setter
@NoArgsConstructor
public class Menu extends BaseAuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "restaurant_id", nullable = false)
    private UUID restaurantId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CatalogStatus status;

    private LocalDate availableFrom;
    private LocalDate availableUntil;
}
