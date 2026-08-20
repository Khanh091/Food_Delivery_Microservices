package com.khanh.fooddelivery.delivery_service.model;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID; import lombok.*; import org.hibernate.annotations.CreationTimestamp; import org.hibernate.annotations.UpdateTimestamp;
@Entity @Table(name="deliveries") @Getter @Setter @NoArgsConstructor public class Delivery {
 @Id private UUID id; @Version private Long version; @Column(nullable=false,unique=true) private UUID orderId;
 @Column(nullable=false) private UUID restaurantId; @Column(nullable=false) private UUID branchId; @Column(nullable=false) private UUID customerId; private UUID driverId;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private DeliveryStatus status; @Column(nullable=false) private String restaurantName; @Column(nullable=false) private String branchName; @Column(nullable=false,length=1000) private String customerAddress;
 @CreationTimestamp @Column(nullable=false,updatable=false) private Instant createdAt; @UpdateTimestamp @Column(nullable=false) private Instant updatedAt;
}
