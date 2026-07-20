package com.khanh.fooddelivery.restaurant_service.service.impl;

import com.khanh.fooddelivery.restaurant_service.entity.RestaurantBranch;
import com.khanh.fooddelivery.restaurant_service.enums.*;
import com.khanh.fooddelivery.restaurant_service.exception.*;
import com.khanh.fooddelivery.restaurant_service.repository.*;
import com.khanh.fooddelivery.restaurant_service.service.RestaurantAuthorizationService;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RestaurantAuthorizationServiceImpl implements RestaurantAuthorizationService {
    private final RestaurantRepository restaurants;
    private final RestaurantMemberRepository members;
    private final RestaurantBranchRepository branches;

    public boolean isOwner(UUID restaurantId, UUID userId) {
        return restaurants
                .findById(restaurantId)
                .map(r -> r.getOwnerUserId().equals(userId))
                .orElse(false);
    }

    public boolean hasRestaurantRole(
            UUID restaurantId, UUID userId, RestaurantMemberRole... roles) {
        return isOwner(restaurantId, userId)
                || members.existsByRestaurantIdAndUserIdAndStatusAndRoleIn(
                        restaurantId, userId, RestaurantMemberStatus.ACTIVE, List.of(roles));
    }

    public boolean hasBranchAccess(UUID branchId, UUID userId, RestaurantMemberRole... roles) {
        RestaurantBranch b = branches.findById(branchId).orElse(null);
        return b != null
                && (hasRestaurantRole(b.getRestaurant().getId(), userId, roles)
                        || members.existsByBranchIdAndUserIdAndStatusAndRoleIn(
                                branchId, userId, RestaurantMemberStatus.ACTIVE, List.of(roles)));
    }

    public void requireRestaurantAccess(UUID id, UUID userId, RestaurantMemberRole... roles) {
        if (!hasRestaurantRole(id, userId, roles))
            throw new AppException(ErrorCode.RESTAURANT_ACCESS_DENIED);
    }

    public void requireBranchAccess(UUID id, UUID userId, RestaurantMemberRole... roles) {
        if (!hasBranchAccess(id, userId, roles))
            throw new AppException(ErrorCode.RESTAURANT_ACCESS_DENIED);
    }
}
