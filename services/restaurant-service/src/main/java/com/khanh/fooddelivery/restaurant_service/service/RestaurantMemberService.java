package com.khanh.fooddelivery.restaurant_service.service;

import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantMemberCreateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantMemberUpdateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantMemberManagementResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;

public interface RestaurantMemberService {
    RestaurantMemberManagementResponse create(
            Jwt jwt, UUID restaurantId, RestaurantMemberCreateRequest r);

    Page<RestaurantMemberManagementResponse> list(Jwt jwt, UUID restaurantId, Pageable p);

    RestaurantMemberManagementResponse update(
            Jwt jwt, UUID restaurantId, UUID memberId, RestaurantMemberUpdateRequest r);

    void remove(Jwt jwt, UUID restaurantId, UUID memberId);
}
