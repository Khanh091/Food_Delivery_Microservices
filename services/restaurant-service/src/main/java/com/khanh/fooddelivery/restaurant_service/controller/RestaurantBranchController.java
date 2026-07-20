package com.khanh.fooddelivery.restaurant_service.controller;

import com.khanh.fooddelivery.restaurant_service.common.response.ApiResponse;
import com.khanh.fooddelivery.restaurant_service.dto.request.AcceptingOrdersUpdateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.BranchBusinessHoursUpdateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.BranchSpecialHourCreateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.BranchSpecialHourUpdateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantBranchCreateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantBranchUpdateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.response.BranchBusinessHourResponse;
import com.khanh.fooddelivery.restaurant_service.dto.response.BranchOperatingStatusResponse;
import com.khanh.fooddelivery.restaurant_service.dto.response.BranchSpecialHourResponse;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantBranchResponse;
import com.khanh.fooddelivery.restaurant_service.service.BranchOperatingStatusService;
import com.khanh.fooddelivery.restaurant_service.service.RestaurantBranchService;
import jakarta.validation.Valid;
import java.time.ZonedDateTime;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RestaurantBranchController {
    private final RestaurantBranchService service;
    private final BranchOperatingStatusService operating;

    @PostMapping("/api/v1/restaurants/{restaurantId}/branches")
    public ResponseEntity<ApiResponse<RestaurantBranchResponse>> create(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID restaurantId,
            @Valid @RequestBody RestaurantBranchCreateRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Restaurant branch created successfully",
                                service.create(jwt, restaurantId, r)));
    }

    @GetMapping("/api/v1/restaurants/{restaurantId}/branches")
    public ApiResponse<List<RestaurantBranchResponse>> list(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID restaurantId) {
        return ApiResponse.success(
                "Restaurant branches retrieved successfully", service.list(jwt, restaurantId));
    }

    @GetMapping("/api/v1/restaurant-branches/{id}")
    public ApiResponse<RestaurantBranchResponse> get(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ApiResponse.success(
                "Restaurant branch retrieved successfully", service.get(jwt, id));
    }

    @PatchMapping("/api/v1/restaurant-branches/{id}")
    public ApiResponse<RestaurantBranchResponse> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody RestaurantBranchUpdateRequest r) {
        return ApiResponse.success(
                "Restaurant branch updated successfully", service.update(jwt, id, r));
    }

    @DeleteMapping("/api/v1/restaurant-branches/{id}")
    public ApiResponse<Void> close(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        service.close(jwt, id);
        return ApiResponse.success("Restaurant branch closed successfully");
    }

    @PatchMapping("/api/v1/restaurant-branches/{id}/accepting-orders")
    public ApiResponse<RestaurantBranchResponse> accepting(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody AcceptingOrdersUpdateRequest r) {
        return ApiResponse.success("Accepting orders updated", service.acceptingOrders(jwt, id, r));
    }

    @PutMapping("/api/v1/restaurant-branches/{id}/business-hours")
    public ApiResponse<List<BranchBusinessHourResponse>> setHours(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody BranchBusinessHoursUpdateRequest r) {
        return ApiResponse.success("Business hours updated", service.setHours(jwt, id, r));
    }

    @GetMapping("/api/v1/restaurant-branches/{id}/business-hours")
    public ApiResponse<List<BranchBusinessHourResponse>> hours(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ApiResponse.success("Business hours retrieved", service.hours(jwt, id));
    }

    @PostMapping("/api/v1/restaurant-branches/{id}/special-hours")
    public ResponseEntity<ApiResponse<BranchSpecialHourResponse>> addSpecial(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody BranchSpecialHourCreateRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Special hours created", service.addSpecial(jwt, id, r)));
    }

    @GetMapping("/api/v1/restaurant-branches/{id}/special-hours")
    public ApiResponse<List<BranchSpecialHourResponse>> specials(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ApiResponse.success("Special hours retrieved", service.specials(jwt, id));
    }

    @PatchMapping("/api/v1/restaurant-branches/{id}/special-hours/{sid}")
    public ApiResponse<BranchSpecialHourResponse> updateSpecial(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @PathVariable UUID sid,
            @Valid @RequestBody BranchSpecialHourUpdateRequest r) {
        return ApiResponse.success("Special hours updated", service.updateSpecial(jwt, id, sid, r));
    }

    @DeleteMapping("/api/v1/restaurant-branches/{id}/special-hours/{sid}")
    public ApiResponse<Void> deleteSpecial(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @PathVariable UUID sid) {
        service.deleteSpecial(jwt, id, sid);
        return ApiResponse.success("Special hours deleted");
    }

    @GetMapping("/api/v1/restaurant-branches/{id}/operating-status")
    public ApiResponse<BranchOperatingStatusResponse> status(@PathVariable UUID id) {
        return ApiResponse.success(
                "Operating status evaluated",
                operating.getOperatingStatus(id, ZonedDateTime.now()));
    }
}
