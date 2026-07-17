package com.khanh.fooddelivery.user_service.service;

import com.khanh.fooddelivery.user_service.dto.request.UserAddressCreateRequest;
import com.khanh.fooddelivery.user_service.dto.request.UserAddressUpdateRequest;
import com.khanh.fooddelivery.user_service.dto.response.UserAddressResponse;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.UUID;

public interface UserAddressService {

    List<UserAddressResponse> getMyAddresses(Jwt jwt);

    UserAddressResponse createAddress(
            Jwt jwt,
            UserAddressCreateRequest request
    );

    UserAddressResponse updateAddress(
            Jwt jwt,
            UUID addressId,
            UserAddressUpdateRequest request
    );

    void deleteAddress(Jwt jwt, UUID addressId);

    UserAddressResponse setDefaultAddress(Jwt jwt, UUID addressId);
}
