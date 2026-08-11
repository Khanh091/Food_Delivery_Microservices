package com.khanh.fooddelivery.restaurant_service.controller;

import com.khanh.fooddelivery.restaurant_service.common.response.ApiResponse;
import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantMemberCreateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantMemberUpdateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantMemberResponse;
import com.khanh.fooddelivery.restaurant_service.service.RestaurantMemberService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping("/api/v1/restaurants/{restaurantId}/members")
@RequiredArgsConstructor
public class RestaurantMemberController {
    private final RestaurantMemberService service;

    @PostMapping
    public ResponseEntity<ApiResponse<RestaurantMemberResponse>> create(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID restaurantId,
            @Valid @RequestBody RestaurantMemberCreateRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Restaurant member invited", service.create(jwt, restaurantId, r)));
    }

    @GetMapping
    public ApiResponse<Page<RestaurantMemberResponse>> list(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID restaurantId,
            @PageableDefault(size = 20) Pageable p) {
        return ApiResponse.success(
                "Restaurant members retrieved", service.list(jwt, restaurantId, p));
    }

    @PatchMapping("/{memberId}")
    public ApiResponse<RestaurantMemberResponse> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID restaurantId,
            @PathVariable UUID memberId,
            @Valid @RequestBody RestaurantMemberUpdateRequest r) {
        return ApiResponse.success(
                "Restaurant member updated", service.update(jwt, restaurantId, memberId, r));
    }

    @DeleteMapping("/{memberId}")
    public ApiResponse<Void> remove(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID restaurantId,
            @PathVariable UUID memberId) {
        service.remove(jwt, restaurantId, memberId);
        return ApiResponse.success("Restaurant member removed");
    }
}
