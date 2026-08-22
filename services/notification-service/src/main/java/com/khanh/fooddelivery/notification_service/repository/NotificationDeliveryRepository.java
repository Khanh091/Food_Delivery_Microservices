package com.khanh.fooddelivery.notification_service.repository;

import com.khanh.fooddelivery.notification_service.entity.NotificationDelivery;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, UUID> {

    Optional<NotificationDelivery> findBySourceEventId(UUID sourceEventId);
}
