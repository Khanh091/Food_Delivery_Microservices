package com.khanh.fooddelivery.restaurant_service.repository;

import com.khanh.fooddelivery.restaurant_service.entity.RestaurantBranch;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RestaurantBranchRepository extends JpaRepository<RestaurantBranch, UUID> {
    List<RestaurantBranch> findAllByRestaurantIdOrderByCreatedAtAsc(UUID id);

    Optional<RestaurantBranch> findByIdAndRestaurantId(UUID id, UUID restaurantId);

    @Query("select branch from RestaurantBranch branch join fetch branch.restaurant where branch.id = :branchId")
    Optional<RestaurantBranch> findByIdWithRestaurant(@Param("branchId") UUID branchId);

    @Query(
            "select branch from RestaurantBranch branch "
                    + "join fetch branch.restaurant restaurant "
                    + "where branch.id = :branchId and restaurant.id = :restaurantId")
    Optional<RestaurantBranch> findPublicByIdAndRestaurantId(
            @Param("restaurantId") UUID restaurantId, @Param("branchId") UUID branchId);

    boolean existsByRestaurantIdAndBranchCode(UUID restaurantId, String branchCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select branch from RestaurantBranch branch join fetch branch.restaurant where branch.id = :id")
    Optional<RestaurantBranch> findByIdForUpdate(@Param("id") UUID id);

    @Query("select branch.id from RestaurantBranch branch where (:afterId is null or branch.id > :afterId) order by branch.id")
    List<UUID> findSnapshotIds(@Param("afterId") UUID afterId, org.springframework.data.domain.Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select branch from RestaurantBranch branch join fetch branch.restaurant where branch.id in :ids order by branch.id")
    List<RestaurantBranch> findAllByIdInForUpdate(@Param("ids") List<UUID> ids);
}
