package com.khanh.fooddelivery.restaurant_service.entity;
import com.khanh.fooddelivery.restaurant_service.common.entity.BaseAuditEntity; import jakarta.persistence.*; import lombok.*; import java.time.LocalTime; import java.util.UUID;
@Entity @Table(name="branch_business_hours") @Getter @Setter @NoArgsConstructor
public class BranchBusinessHour extends BaseAuditEntity {@Id private UUID id; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="branch_id") private RestaurantBranch branch; @Column(nullable=false) private Short dayOfWeek; private LocalTime openTime; private LocalTime closeTime; @Column(nullable=false) private boolean closed; @Version private long version; @PrePersist void id(){if(id==null)id=UUID.randomUUID();}}
