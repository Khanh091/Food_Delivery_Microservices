package com.khanh.fooddelivery.order_service.repository;

import com.khanh.fooddelivery.order_service.entity.Order;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    @EntityGraph(attributePaths = {"items"}) Optional<Order> findWithItemsById(UUID id);
    @EntityGraph(attributePaths = {"items"}) List<Order> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
    @EntityGraph(attributePaths = {"items"}) List<Order> findByRestaurantIdOrderByCreatedAtDesc(UUID restaurantId);
}
