package com.khanh.fooddelivery.catalog_service.entity;

import com.khanh.fooddelivery.catalog_service.common.entity.BaseAuditEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "option_values")
@Getter
@Setter
@NoArgsConstructor
public class OptionValue extends BaseAuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "option_group_id", nullable = false)
    private OptionGroup optionGroup;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "additional_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal additionalPrice = BigDecimal.ZERO;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = true;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
