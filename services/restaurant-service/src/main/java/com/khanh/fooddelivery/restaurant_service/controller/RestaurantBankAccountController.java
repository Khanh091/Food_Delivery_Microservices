package com.khanh.fooddelivery.restaurant_service.controller;

import com.khanh.fooddelivery.restaurant_service.common.response.ApiResponse;
import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantBankAccountCreateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantBankAccountUpdateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantBankAccountResponse;
import com.khanh.fooddelivery.restaurant_service.service.RestaurantBankAccountService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
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

@RestController
@RequestMapping("/api/v1/restaurants/{restaurantId}/bank-accounts")
@RequiredArgsConstructor
public class RestaurantBankAccountController {
    private final RestaurantBankAccountService service;

    @PostMapping
    public ResponseEntity<ApiResponse<RestaurantBankAccountResponse>> create(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID restaurantId,
            @Valid @RequestBody RestaurantBankAccountCreateRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Bank account created", service.create(jwt, restaurantId, r)));
    }

    @GetMapping
    public ApiResponse<List<RestaurantBankAccountResponse>> list(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID restaurantId) {
        return ApiResponse.success("Bank accounts retrieved", service.list(jwt, restaurantId));
    }

    @PatchMapping("/{accountId}")
    public ApiResponse<RestaurantBankAccountResponse> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID restaurantId,
            @PathVariable UUID accountId,
            @Valid @RequestBody RestaurantBankAccountUpdateRequest r) {
        return ApiResponse.success(
                "Bank account updated", service.update(jwt, restaurantId, accountId, r));
    }

    @DeleteMapping("/{accountId}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID restaurantId,
            @PathVariable UUID accountId) {
        service.delete(jwt, restaurantId, accountId);
        return ApiResponse.success("Bank account deleted");
    }

    @PostMapping("/{accountId}/default")
    public ApiResponse<RestaurantBankAccountResponse> setDefault(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID restaurantId,
            @PathVariable UUID accountId) {
        return ApiResponse.success(
                "Default bank account updated", service.setDefault(jwt, restaurantId, accountId));
    }
}
