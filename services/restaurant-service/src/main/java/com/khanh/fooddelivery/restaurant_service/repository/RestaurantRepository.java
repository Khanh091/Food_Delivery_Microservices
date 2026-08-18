package com.khanh.fooddelivery.restaurant_service.repository;

import com.khanh.fooddelivery.restaurant_service.entity.Restaurant;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantMemberStatus;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantStatus;
import java.util.Collection;
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

    @Query("""
            select distinct restaurant from Restaurant restaurant
            left join RestaurantMember member on member.restaurant = restaurant
            where restaurant.ownerUserId = :userId
               or (member.userId = :userId and member.status = :memberStatus)
            order by restaurant.createdAt desc
            """)
    List<Restaurant> findAllManageableByUserIdOrderByCreatedAtDesc(
            @Param("userId") UUID userId,
            @Param("memberStatus") RestaurantMemberStatus memberStatus);

    Optional<Restaurant> findByPartnerApplicationId(UUID id);

    boolean existsByRestaurantCode(String code);

    @Query("select distinct restaurant.ownerUserId from Restaurant restaurant where restaurant.status in :statuses")
    List<UUID> findOwnerUserIdsByStatusIn(@Param("statuses") Collection<RestaurantStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select restaurant from Restaurant restaurant where restaurant.id = :id")
    Optional<Restaurant> findByIdForUpdate(@Param("id") UUID id);

    @Query("select restaurant.id from Restaurant restaurant where (:afterId is null or restaurant.id > :afterId) order by restaurant.id")
    List<UUID> findSnapshotIds(@Param("afterId") UUID afterId, org.springframework.data.domain.Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select restaurant from Restaurant restaurant where restaurant.id in :ids order by restaurant.id")
    List<Restaurant> findAllByIdInForUpdate(@Param("ids") List<UUID> ids);
}
