package com.khanh.fooddelivery.driver_service.repository;

import com.khanh.fooddelivery.driver_service.entity.DriverProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverProfileRepository extends JpaRepository<DriverProfile, UUID> {
    Optional<DriverProfile> findByUserId(UUID userId);
}
