package com.khanh.fooddelivery.driver_service.entity;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID; import lombok.*; import org.hibernate.annotations.UpdateTimestamp;
@Entity @Table(name="driver_availability") @Getter @Setter @NoArgsConstructor public class DriverAvailability { @Id private UUID id; @Version private Long version; @Column(nullable=false,unique=true) private UUID userId; @Column(nullable=false) private boolean available; private UUID activeDeliveryId; @UpdateTimestamp @Column(nullable=false) private Instant updatedAt; }
