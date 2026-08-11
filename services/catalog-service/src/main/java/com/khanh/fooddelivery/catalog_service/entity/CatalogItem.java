package com.khanh.fooddelivery.catalog_service.entity;

import com.khanh.fooddelivery.catalog_service.common.entity.BaseAuditEntity;
import com.khanh.fooddelivery.catalog_service.enums.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "catalog_items")
@Getter
@Setter
@NoArgsConstructor
public class CatalogItem extends BaseAuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "restaurant_id", nullable = false)
    private UUID restaurantId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 30)
    private CatalogItemType itemType;

    @Column(name = "base_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal basePrice;

    @Column(nullable = false, length = 3)
    private String currency = "VND";

    @Column(name = "preparation_time_minutes")
    private Integer preparationTimeMinutes;

    @Column(name = "is_vegetarian", nullable = false)
    private Boolean isVegetarian = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CatalogStatus status;
}
