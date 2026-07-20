package com.khanh.fooddelivery.restaurant_service.repository;

import com.khanh.fooddelivery.restaurant_service.entity.RestaurantMember;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantMemberRole;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantMemberStatus;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantMemberRepository extends JpaRepository<RestaurantMember, UUID> {
    Optional<RestaurantMember> findByRestaurantIdAndUserIdAndBranchIdIsNull(
            UUID restaurantId, UUID userId);

    Optional<RestaurantMember> findByRestaurantIdAndBranchIdAndUserId(
            UUID restaurantId, UUID branchId, UUID userId);

    Optional<RestaurantMember> findByIdAndRestaurantId(UUID id, UUID restaurantId);

    Page<RestaurantMember> findAllByRestaurantId(UUID restaurantId, Pageable pageable);

    boolean existsByRestaurantIdAndUserIdAndBranchIdIsNull(UUID restaurantId, UUID userId);

    boolean existsByRestaurantIdAndBranchIdAndUserId(UUID restaurantId, UUID branchId, UUID userId);

    boolean existsByRestaurantIdAndUserIdAndStatusAndRoleIn(
            UUID restaurantId,
            UUID userId,
            RestaurantMemberStatus status,
            Collection<RestaurantMemberRole> roles);

    boolean existsByBranchIdAndUserIdAndStatusAndRoleIn(
            UUID branchId,
            UUID userId,
            RestaurantMemberStatus status,
            Collection<RestaurantMemberRole> roles);
}
