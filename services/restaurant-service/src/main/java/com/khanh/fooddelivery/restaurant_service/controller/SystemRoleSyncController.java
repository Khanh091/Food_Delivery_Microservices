package com.khanh.fooddelivery.restaurant_service.controller;

import com.khanh.fooddelivery.restaurant_service.common.response.ApiResponse;
import com.khanh.fooddelivery.restaurant_service.dto.response.SystemRoleReconcileResponse;
import com.khanh.fooddelivery.restaurant_service.service.SystemRoleSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/restaurants")
@RequiredArgsConstructor
public class SystemRoleSyncController {
    private final SystemRoleSyncService systemRoleSync;

    @PostMapping("/system-role-sync/reconcile-owners")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT')")
    public ApiResponse<SystemRoleReconcileResponse> reconcileOwners() {
        return ApiResponse.success(
                "Restaurant owner system roles enqueued",
                new SystemRoleReconcileResponse(systemRoleSync.reconcileRestaurantOwners()));
    }
}