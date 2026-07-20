package com.khanh.fooddelivery.restaurant_service.service;

import com.khanh.fooddelivery.restaurant_service.dto.request.ApplicationInformationRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.ApplicationRejectionRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantApplicationCreateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantApplicationUpdateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantApplicationResponse;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantApplicationSummaryResponse;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;

public interface RestaurantApplicationService {
    RestaurantApplicationResponse create(Jwt jwt, RestaurantApplicationCreateRequest r);

    List<RestaurantApplicationSummaryResponse> mine(Jwt jwt);

    RestaurantApplicationResponse get(Jwt jwt, UUID id, boolean admin);

    Page<RestaurantApplicationSummaryResponse> all(Pageable p);

    RestaurantApplicationResponse update(Jwt jwt, UUID id, RestaurantApplicationUpdateRequest r);

    RestaurantApplicationResponse submit(Jwt jwt, UUID id);

    RestaurantApplicationResponse cancel(Jwt jwt, UUID id);

    RestaurantApplicationResponse startReview(Jwt jwt, UUID id);

    RestaurantApplicationResponse requestInformation(
            Jwt jwt, UUID id, ApplicationInformationRequest r);

    RestaurantResponse approve(Jwt jwt, UUID id);

    RestaurantApplicationResponse reject(Jwt jwt, UUID id, ApplicationRejectionRequest r);
}
