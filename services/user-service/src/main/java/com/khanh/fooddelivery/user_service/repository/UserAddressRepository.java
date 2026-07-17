package com.khanh.fooddelivery.user_service.repository;

import com.khanh.fooddelivery.user_service.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAddressRepository
        extends JpaRepository<UserAddress, UUID> {

    List<UserAddress> findAllByUserIdOrderByIsDefaultDescCreatedAtDesc(
            UUID userId
    );

    Optional<UserAddress> findByIdAndUserId(
            UUID addressId,
            UUID userId
    );

    Optional<UserAddress> findByUserIdAndIsDefaultTrue(UUID userId);

    Optional<UserAddress> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

    boolean existsByUserId(UUID userId);
}
