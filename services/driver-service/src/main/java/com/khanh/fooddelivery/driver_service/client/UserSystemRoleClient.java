package com.khanh.fooddelivery.driver_service.client;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service")
public interface UserSystemRoleClient {
    @PostMapping("/internal/v1/users/{userId}/system-roles/DRIVER")
    void grantDriverRole(@PathVariable UUID userId, @RequestHeader("X-Internal-Api-Key") String internalApiKey);
}
