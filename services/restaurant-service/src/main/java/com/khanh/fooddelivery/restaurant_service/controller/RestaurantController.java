package com.khanh.fooddelivery.restaurant_service.controller;

import com.khanh.fooddelivery.restaurant_service.common.response.ApiResponse;
import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantUpdateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantResponse;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantStatusHistoryResponse;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantSummaryResponse;
import com.khanh.fooddelivery.restaurant_service.service.RestaurantService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/restaurants")
@RequiredArgsConstructor
public class RestaurantController {
    private final RestaurantService service;

    @GetMapping("/me")
    public ApiResponse<List<RestaurantSummaryResponse>> mine(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success("Restaurants retrieved successfully", service.mine(jwt));
    }

    @GetMapping("/{id}")
    public ApiResponse<RestaurantResponse> get(
            @AuthenticationPrincipal Jwt jwt, Authentication a, @PathVariable UUID id) {
        return ApiResponse.success(
                "Restaurant retrieved successfully", service.get(jwt, id, isAdmin(a)));
    }

    @PatchMapping("/{id}")
    public ApiResponse<RestaurantResponse> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody RestaurantUpdateRequest r) {
        return ApiResponse.success("Restaurant updated successfully", service.update(jwt, id, r));
    }

    @PostMapping("/{id}/activate")
    public ApiResponse<RestaurantResponse> activate(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ApiResponse.success("Restaurant activated", service.activate(jwt, id));
    }

    @PostMapping("/{id}/deactivate")
    public ApiResponse<RestaurantResponse> deactivate(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ApiResponse.success("Restaurant deactivated", service.deactivate(jwt, id));
    }

    @PostMapping("/{id}/close")
    public ApiResponse<RestaurantResponse> close(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        return ApiResponse.success("Restaurant closed", service.close(jwt, id, reason));
    }

    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT')")
    public ApiResponse<RestaurantResponse> suspend(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        return ApiResponse.success("Restaurant suspended", service.suspend(jwt, id, reason));
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT')")
    public ApiResponse<RestaurantResponse> restore(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestParam(defaultValue = "ACTIVE")
                    com.khanh.fooddelivery.restaurant_service.enums.RestaurantStatus targetStatus,
            @RequestParam(required = false) String reason) {
        return ApiResponse.success(
                "Restaurant restored", service.restore(jwt, id, targetStatus, reason));
    }

    @GetMapping("/{id}/status-history")
    public ApiResponse<Page<RestaurantStatusHistoryResponse>> history(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @PageableDefault(size = 20) Pageable p) {
        return ApiResponse.success(
                "Status history retrieved successfully", service.history(jwt, id, p));
    }

    private boolean isAdmin(Authentication a) {
        return a.getAuthorities().stream()
                .anyMatch(
                        x ->
                                x.getAuthority().equals("ROLE_ADMIN")
                                        || x.getAuthority().equals("ROLE_SUPPORT"));
    }
}
