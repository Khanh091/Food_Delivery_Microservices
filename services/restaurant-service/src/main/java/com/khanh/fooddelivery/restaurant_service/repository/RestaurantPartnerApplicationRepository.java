package com.khanh.fooddelivery.restaurant_service.repository;

import com.khanh.fooddelivery.restaurant_service.entity.RestaurantPartnerApplication;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantPartnerApplicationRepository
        extends JpaRepository<RestaurantPartnerApplication, UUID> {
    List<RestaurantPartnerApplication> findAllByApplicantUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<RestaurantPartnerApplication> findByIdAndApplicantUserId(UUID id, UUID userId);

    Page<RestaurantPartnerApplication> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
