package com.khanh.fooddelivery.order_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
@Table(name = "order_item_options")
@Getter
@Setter
@NoArgsConstructor
public class OrderItemOption {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Column(nullable = false)
    private UUID optionGroupId;

    @Column(nullable = false)
    private UUID optionValueId;

    @Column(nullable = false)
    private String optionGroupName;

    @Column(nullable = false)
    private String optionValueName;

    @Column(nullable = false)
    private BigDecimal additionalPrice;

    @Column(nullable = false)
    private Integer sortOrder;
}