package com.khanh.fooddelivery.delivery_service.repository;
import com.khanh.fooddelivery.delivery_service.model.Delivery; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface DeliveryRepository extends JpaRepository<Delivery,UUID> { Optional<Delivery> findByOrderId(UUID orderId); List<Delivery> findByStatusOrderByCreatedAtAsc(com.khanh.fooddelivery.delivery_service.model.DeliveryStatus status); }
