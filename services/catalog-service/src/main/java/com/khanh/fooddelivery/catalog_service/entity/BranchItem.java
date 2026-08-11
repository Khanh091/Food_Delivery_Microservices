package com.khanh.fooddelivery.catalog_service.entity;

import com.khanh.fooddelivery.catalog_service.common.entity.BaseAuditEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "branch_items")
@Getter
@Setter
@NoArgsConstructor
public class BranchItem extends BaseAuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private CatalogItem item;

    @Column(name = "selling_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal sellingPrice;

    @Column(name = "original_price", precision = 19, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = true;

    @Column(name = "available_quantity")
    private Integer availableQuantity;

    @Column(name = "sold_out_until")
    private Instant soldOutUntil;
}
