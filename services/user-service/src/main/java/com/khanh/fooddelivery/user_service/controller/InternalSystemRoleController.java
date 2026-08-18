package com.khanh.fooddelivery.user_service.controller;

import com.khanh.fooddelivery.user_service.common.response.ApiResponse;
import com.khanh.fooddelivery.user_service.config.InternalApiProperties;
import com.khanh.fooddelivery.user_service.enums.SystemRole;
import com.khanh.fooddelivery.user_service.exception.AppException;
import com.khanh.fooddelivery.user_service.exception.ErrorCode;
import com.khanh.fooddelivery.user_service.service.SystemRoleProvisioningService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/users/{userId}/system-roles")
@RequiredArgsConstructor
public class InternalSystemRoleController {
    public static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final SystemRoleProvisioningService systemRoles;
    private final InternalApiProperties internalApi;

    @PostMapping("/{role}")
    public ApiResponse<Void> grant(
            @PathVariable UUID userId,
            @PathVariable SystemRole role,
            @RequestHeader(name = INTERNAL_API_KEY_HEADER, required = false) String apiKey) {
        authenticate(apiKey);
        systemRoles.grantRole(userId, role);
        return ApiResponse.success("System role ensured");
    }

    @DeleteMapping("/{role}")
    public ApiResponse<Void> revoke(
            @PathVariable UUID userId,
            @PathVariable SystemRole role,
            @RequestHeader(name = INTERNAL_API_KEY_HEADER, required = false) String apiKey) {
        authenticate(apiKey);
        systemRoles.revokeRole(userId, role);
        return ApiResponse.success("System role removed when assigned");
    }

    private void authenticate(String apiKey) {
        byte[] expected = internalApi.getKey().getBytes(StandardCharsets.UTF_8);
        byte[] supplied = (apiKey == null ? "" : apiKey).getBytes(StandardCharsets.UTF_8);
        if (expected.length == 0 || !MessageDigest.isEqual(expected, supplied)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
    }
}
