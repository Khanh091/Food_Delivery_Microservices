package com.khanh.fooddelivery.restaurant_service.service;

import com.khanh.fooddelivery.restaurant_service.dto.request.AcceptingOrdersUpdateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.BranchBusinessHoursUpdateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.BranchSpecialHourCreateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.BranchSpecialHourUpdateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantBranchCreateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantBranchUpdateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.response.BranchBusinessHourResponse;
import com.khanh.fooddelivery.restaurant_service.dto.response.BranchSpecialHourResponse;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantBranchResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

public interface RestaurantBranchService {
    RestaurantBranchResponse create(Jwt jwt, UUID restaurantId, RestaurantBranchCreateRequest r);

    List<RestaurantBranchResponse> list(Jwt jwt, UUID restaurantId);

    RestaurantBranchResponse get(Jwt jwt, UUID id);

    RestaurantBranchResponse update(Jwt jwt, UUID id, RestaurantBranchUpdateRequest r);

    void close(Jwt jwt, UUID id);

    RestaurantBranchResponse acceptingOrders(Jwt jwt, UUID id, AcceptingOrdersUpdateRequest r);

    List<BranchBusinessHourResponse> setHours(Jwt jwt, UUID id, BranchBusinessHoursUpdateRequest r);

    List<BranchBusinessHourResponse> hours(Jwt jwt, UUID id);

    BranchSpecialHourResponse addSpecial(Jwt jwt, UUID id, BranchSpecialHourCreateRequest r);

    List<BranchSpecialHourResponse> specials(Jwt jwt, UUID id);

    BranchSpecialHourResponse updateSpecial(
            Jwt jwt, UUID id, UUID specialId, BranchSpecialHourUpdateRequest r);

    void deleteSpecial(Jwt jwt, UUID id, UUID specialId);
}
