package com.khanh.fooddelivery.restaurant_service.repository;
import com.khanh.fooddelivery.restaurant_service.entity.RestaurantBranch; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface RestaurantBranchRepository extends JpaRepository<RestaurantBranch,UUID>{List<RestaurantBranch> findAllByRestaurantIdOrderByCreatedAtAsc(UUID id);Optional<RestaurantBranch> findByIdAndRestaurantId(UUID id,UUID restaurantId);boolean existsByRestaurantIdAndBranchCode(UUID restaurantId,String branchCode);}
