package com.khanh.fooddelivery.catalog_service.entity;

import com.khanh.fooddelivery.catalog_service.common.entity.BaseAuditEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "item_price_histories")
@Getter
@Setter
@NoArgsConstructor
public class ItemPriceHistory extends BaseAuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_item_id", nullable = false)
    private BranchItem branchItem;

    @Column(name = "old_price", precision = 19, scale = 2)
    private BigDecimal oldPrice;

    @Column(name = "new_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal newPrice;

    @Column(length = 500)
    private String reason;

    @Column(name = "changed_by")
    private UUID changedBy;
}
