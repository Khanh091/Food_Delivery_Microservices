package com.khanh.fooddelivery.restaurant_service.entity;
import com.khanh.fooddelivery.restaurant_service.common.entity.BaseAuditEntity; import jakarta.persistence.*; import lombok.*; import java.time.*; import java.util.UUID;
@Entity @Table(name="branch_special_hours") @Getter @Setter @NoArgsConstructor
public class BranchSpecialHour extends BaseAuditEntity {@Id private UUID id; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="branch_id") private RestaurantBranch branch; @Column(nullable=false) private LocalDate specialDate; private LocalTime openTime; private LocalTime closeTime; @Column(nullable=false) private boolean closed; private String reason; @Version private long version; @PrePersist void id(){if(id==null)id=UUID.randomUUID();}}
