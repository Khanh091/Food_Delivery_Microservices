package com.khanh.fooddelivery.cart_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service")
public interface UserServiceClient {
    @GetMapping("/api/v1/users/me")
    ApiResponse<CurrentUserResponse> getCurrentUser(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ApiResponse<T>(boolean success, String code, String message, T data, Instant timestamp) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CurrentUserResponse(UUID id) {}
}
