package com.khanh.fooddelivery.driver_service.repository;
import com.khanh.fooddelivery.driver_service.entity.DriverAvailability; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface DriverAvailabilityRepository extends JpaRepository<DriverAvailability,UUID> { Optional<DriverAvailability> findByUserId(UUID userId); List<DriverAvailability> findTop10ByAvailableTrueAndActiveDeliveryIdIsNullOrderByUpdatedAtAsc(); }
