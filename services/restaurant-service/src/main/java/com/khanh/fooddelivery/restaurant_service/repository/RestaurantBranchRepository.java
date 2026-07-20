package com.khanh.fooddelivery.restaurant_service.repository;

import com.khanh.fooddelivery.restaurant_service.entity.RestaurantBranch;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantBranchRepository extends JpaRepository<RestaurantBranch, UUID> {
    List<RestaurantBranch> findAllByRestaurantIdOrderByCreatedAtAsc(UUID id);

    Optional<RestaurantBranch> findByIdAndRestaurantId(UUID id, UUID restaurantId);

    boolean existsByRestaurantIdAndBranchCode(UUID restaurantId, String branchCode);
}
