package com.khanh.fooddelivery.catalog_service.entity;

import com.khanh.fooddelivery.catalog_service.common.entity.BaseAuditEntity;
import com.khanh.fooddelivery.catalog_service.enums.CatalogStatus;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "menu_categories")
@Getter
@Setter
@NoArgsConstructor
public class MenuCategory extends BaseAuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CatalogStatus status;
}
