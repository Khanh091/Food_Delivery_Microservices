package com.khanh.fooddelivery.restaurant_service.repository;
import com.khanh.fooddelivery.restaurant_service.entity.Restaurant; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface RestaurantRepository extends JpaRepository<Restaurant,UUID>{boolean existsByPartnerApplicationId(UUID id);List<Restaurant> findAllByOwnerUserIdOrderByCreatedAtDesc(UUID userId);Optional<Restaurant> findByPartnerApplicationId(UUID id);boolean existsByRestaurantCode(String code);}
