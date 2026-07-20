package com.khanh.fooddelivery.restaurant_service.repository;

import com.khanh.fooddelivery.restaurant_service.entity.RestaurantStatusHistory;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantStatusHistoryRepository
        extends JpaRepository<RestaurantStatusHistory, UUID> {
    Page<RestaurantStatusHistory> findAllByRestaurantIdOrderByChangedAtDesc(
            UUID id, Pageable pageable);
}
