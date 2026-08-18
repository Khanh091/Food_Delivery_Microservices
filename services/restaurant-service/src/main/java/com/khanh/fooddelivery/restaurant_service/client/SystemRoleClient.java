package com.khanh.fooddelivery.restaurant_service.client;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service", contextId = "user-service-system-roles", path = "/internal/v1/users")
public interface SystemRoleClient {
    String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    @PostMapping("/{userId}/system-roles/{role}")
    void grantRole(
            @PathVariable UUID userId,
            @PathVariable String role,
            @RequestHeader(INTERNAL_API_KEY_HEADER) String apiKey);

    @DeleteMapping("/{userId}/system-roles/{role}")
    void revokeRole(
            @PathVariable UUID userId,
            @PathVariable String role,
            @RequestHeader(INTERNAL_API_KEY_HEADER) String apiKey);
}