package com.khanh.fooddelivery.user_service.controller;

import com.khanh.fooddelivery.user_service.common.response.ApiResponse;
import com.khanh.fooddelivery.user_service.dto.response.internal.InternalUserAddressResponse;
import com.khanh.fooddelivery.user_service.service.UserAddressService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/users/me/addresses")
@RequiredArgsConstructor
public class InternalUserAddressController {
    private final UserAddressService addressService;

    @GetMapping("/{addressId}")
    public ApiResponse<InternalUserAddressResponse> getOwnedAddress(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID addressId) {
        return ApiResponse.success(
                "Checkout address retrieved", addressService.getMyAddressForCheckout(jwt, addressId));
    }
}
