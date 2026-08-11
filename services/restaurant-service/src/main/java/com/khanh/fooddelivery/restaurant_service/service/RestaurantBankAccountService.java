package com.khanh.fooddelivery.restaurant_service.service;

import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantBankAccountCreateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantBankAccountUpdateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantBankAccountResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

public interface RestaurantBankAccountService {
    RestaurantBankAccountResponse create(
            Jwt jwt, UUID restaurantId, RestaurantBankAccountCreateRequest r);

    List<RestaurantBankAccountResponse> list(Jwt jwt, UUID restaurantId);

    RestaurantBankAccountResponse update(
            Jwt jwt, UUID restaurantId, UUID accountId, RestaurantBankAccountUpdateRequest r);

    void delete(Jwt jwt, UUID restaurantId, UUID accountId);

    RestaurantBankAccountResponse setDefault(Jwt jwt, UUID restaurantId, UUID accountId);
}
