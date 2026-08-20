package com.khanh.fooddelivery.catalog_service.entity;

import com.khanh.fooddelivery.catalog_service.common.entity.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "option_template_values")
@Getter
@Setter
@NoArgsConstructor
public class OptionTemplateValue extends BaseAuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private OptionTemplate template;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "additional_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal additionalPrice = BigDecimal.ZERO;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = true;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
