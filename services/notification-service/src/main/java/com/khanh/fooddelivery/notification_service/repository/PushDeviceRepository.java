package com.khanh.fooddelivery.notification_service.repository;

import com.khanh.fooddelivery.notification_service.entity.PushDevice;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PushDeviceRepository extends JpaRepository<PushDevice, UUID> {

    Optional<PushDevice> findByExpoPushToken(String expoPushToken);

    List<PushDevice> findByDriverIdAndActiveTrue(UUID driverId);

    Optional<PushDevice> findByIdAndDriverId(UUID id, UUID driverId);
}
