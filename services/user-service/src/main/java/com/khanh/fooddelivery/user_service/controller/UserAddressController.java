package com.khanh.fooddelivery.user_service.controller;

import com.khanh.fooddelivery.user_service.common.response.ApiResponse;
import com.khanh.fooddelivery.user_service.dto.request.UserAddressCreateRequest;
import com.khanh.fooddelivery.user_service.dto.request.UserAddressUpdateRequest;
import com.khanh.fooddelivery.user_service.dto.response.UserAddressResponse;
import com.khanh.fooddelivery.user_service.service.UserAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/addresses")
@RequiredArgsConstructor
public class UserAddressController {

    private final UserAddressService addressService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserAddressResponse>>>
    getMyAddresses(@AuthenticationPrincipal Jwt jwt) {
        List<UserAddressResponse> response =
                addressService.getMyAddresses(jwt);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserAddressResponse>> createAddress(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UserAddressCreateRequest request
    ) {
        UserAddressResponse response =
                addressService.createAddress(jwt, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Address created successfully",
                        response
                ));
    }

    @PatchMapping("/{addressId}")
    public ResponseEntity<ApiResponse<UserAddressResponse>> updateAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID addressId,
            @Valid @RequestBody UserAddressUpdateRequest request
    ) {
        UserAddressResponse response =
                addressService.updateAddress(jwt, addressId, request);
        return ResponseEntity.ok(ApiResponse.success(
                "Address updated successfully",
                response
        ));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID addressId
    ) {
        addressService.deleteAddress(jwt, addressId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{addressId}/default")
    public ResponseEntity<ApiResponse<UserAddressResponse>>
    setDefaultAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID addressId
    ) {
        UserAddressResponse response =
                addressService.setDefaultAddress(jwt, addressId);
        return ResponseEntity.ok(ApiResponse.success(
                "Default address updated successfully",
                response
        ));
    }
}
