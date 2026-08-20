package com.khanh.fooddelivery.driver_service.repository;

import com.khanh.fooddelivery.driver_service.entity.DriverAvailability;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DriverAvailabilityRepository
        extends JpaRepository<DriverAvailability, UUID> {

    Optional<DriverAvailability> findByUserId(UUID userId);

    List<DriverAvailability>
    findTop10ByAvailableTrueAndActiveDeliveryIdIsNullAndPendingOfferDeliveryIdIsNullOrderByUpdatedAtAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select d
            from DriverAvailability d
            where d.userId = :userId
            """)
    Optional<DriverAvailability> findByUserIdForUpdate(
            @Param("userId") UUID userId
    );
}