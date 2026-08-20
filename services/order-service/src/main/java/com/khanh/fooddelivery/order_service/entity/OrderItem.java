package com.khanh.fooddelivery.order_service.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "order_items") @Getter @Setter @NoArgsConstructor
public class OrderItem {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "order_id", nullable = false) private Order order;
    @Column(nullable = false) private UUID catalogItemId;
    @Column(nullable = false) private UUID branchItemId;
    @Column(nullable = false) private String itemName;
    @Column(length = 2000) private String imageUrl;
    @Column(nullable = false) private BigDecimal unitPrice;
    @Column(nullable = false) private Integer quantity;
    @Column(nullable = false) private BigDecimal lineTotal;
    @Column(length = 500) private String note;
    @Column(nullable = false) private Integer sortOrder;
    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC") private List<OrderItemOption> options = new ArrayList<>();
}
