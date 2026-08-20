package com.khanh.fooddelivery.delivery_service.repository;

import com.khanh.fooddelivery.delivery_service.model.DeliveryOffer;
import com.khanh.fooddelivery.delivery_service.model.DeliveryOfferStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeliveryOfferRepository
        extends JpaRepository<DeliveryOffer, UUID> {

    Optional<DeliveryOffer> findByDeliveryIdAndDriverIdAndStatus(
            UUID deliveryId,
            UUID driverId,
            DeliveryOfferStatus status
    );

    boolean existsByDriverIdAndStatus(
            UUID driverId,
            DeliveryOfferStatus status
    );

    @Query("""
            select o
            from DeliveryOffer o
            where o.status = 'PENDING'
              and o.expiresAt <= :now
            """)
    List<DeliveryOffer> findExpired(
            @Param("now") Instant now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select o
            from DeliveryOffer o
            where o.id = :id
            """)
    Optional<DeliveryOffer> findByIdForUpdate(
            @Param("id") UUID id
    );
}