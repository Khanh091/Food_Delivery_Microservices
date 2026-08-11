package com.khanh.fooddelivery.restaurant_service.service;

import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantMemberCreateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantMemberUpdateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantMemberResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;

public interface RestaurantMemberService {
    RestaurantMemberResponse create(Jwt jwt, UUID restaurantId, RestaurantMemberCreateRequest r);

    Page<RestaurantMemberResponse> list(Jwt jwt, UUID restaurantId, Pageable p);

    RestaurantMemberResponse update(
            Jwt jwt, UUID restaurantId, UUID memberId, RestaurantMemberUpdateRequest r);

    void remove(Jwt jwt, UUID restaurantId, UUID memberId);
}
