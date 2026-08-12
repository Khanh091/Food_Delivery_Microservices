package com.khanh.fooddelivery.restaurant_service.repository;

import com.khanh.fooddelivery.restaurant_service.entity.Restaurant;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RestaurantRepository extends JpaRepository<Restaurant, UUID> {
    boolean existsByPartnerApplicationId(UUID id);

    List<Restaurant> findAllByOwnerUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Restaurant> findByPartnerApplicationId(UUID id);

    boolean existsByRestaurantCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select restaurant from Restaurant restaurant where restaurant.id = :id")
    Optional<Restaurant> findByIdForUpdate(@Param("id") UUID id);

    @Query("select restaurant.id from Restaurant restaurant where (:afterId is null or restaurant.id > :afterId) order by restaurant.id")
    List<UUID> findSnapshotIds(@Param("afterId") UUID afterId, org.springframework.data.domain.Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select restaurant from Restaurant restaurant where restaurant.id in :ids order by restaurant.id")
    List<Restaurant> findAllByIdInForUpdate(@Param("ids") List<UUID> ids);
}
