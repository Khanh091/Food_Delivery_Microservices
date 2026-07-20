package com.khanh.fooddelivery.restaurant_service.service;

import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantUpdateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantResponse;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantStatusHistoryResponse;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantSummaryResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;

public interface RestaurantService {
    List<RestaurantSummaryResponse> mine(Jwt jwt);

    RestaurantResponse get(Jwt jwt, UUID id, boolean admin);

    RestaurantResponse update(Jwt jwt, UUID id, RestaurantUpdateRequest r);

    RestaurantResponse activate(Jwt jwt, UUID id);

    RestaurantResponse deactivate(Jwt jwt, UUID id);

    RestaurantResponse close(Jwt jwt, UUID id, String reason);

    RestaurantResponse suspend(Jwt jwt, UUID id, String reason);

    RestaurantResponse restore(
            Jwt jwt,
            UUID id,
            com.khanh.fooddelivery.restaurant_service.enums.RestaurantStatus targetStatus,
            String reason);

    Page<RestaurantStatusHistoryResponse> history(Jwt jwt, UUID id, Pageable p);
}
