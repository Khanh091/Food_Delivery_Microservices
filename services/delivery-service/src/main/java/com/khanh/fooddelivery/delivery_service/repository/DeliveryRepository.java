package com.khanh.fooddelivery.delivery_service.repository;

import com.khanh.fooddelivery.delivery_service.model.Delivery;
import com.khanh.fooddelivery.delivery_service.model.DeliveryStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {

    Optional<Delivery> findByOrderId(UUID orderId);

    List<Delivery> findByStatusOrderByCreatedAtAsc(DeliveryStatus status);

    List<Delivery> findByDriverIdAndStatusInOrderByUpdatedAtDesc(
            UUID driverId,
            List<DeliveryStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Delivery d where d.id = :id")
    Optional<Delivery> findByIdForUpdate(@Param("id") UUID id);
}
