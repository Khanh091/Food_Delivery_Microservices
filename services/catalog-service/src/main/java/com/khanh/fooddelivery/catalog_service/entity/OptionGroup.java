package com.khanh.fooddelivery.catalog_service.entity;

import com.khanh.fooddelivery.catalog_service.common.entity.BaseAuditEntity;
import com.khanh.fooddelivery.catalog_service.enums.*;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "option_groups")
@Getter
@Setter
@NoArgsConstructor
public class OptionGroup extends BaseAuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private CatalogItem item;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "selection_type", nullable = false, length = 20)
    private OptionSelectionType selectionType;

    @Column(name = "minimum_selections", nullable = false)
    private Integer minimumSelections = 0;

    @Column(name = "maximum_selections", nullable = false)
    private Integer maximumSelections = 1;

    @Column(name = "required", nullable = false)
    private Boolean required = false;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CatalogStatus status;
}
