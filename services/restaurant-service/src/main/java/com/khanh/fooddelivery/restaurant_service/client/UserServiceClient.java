package com.khanh.fooddelivery.restaurant_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/api/v1/users/me")
    ApiResponse<CurrentUserResponse> getCurrentUser(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization);

    @PostMapping("/internal/v1/users/batch-profile")
    ApiResponse<List<InternalUserProfileResponse>> batchProfiles(
            @RequestHeader("X-Internal-Api-Key") String internalApiKey,
            @RequestBody BatchProfileRequest request);

    @PostMapping("/internal/v1/users/resolve-email")
    ApiResponse<InternalUserProfileResponse> resolveByEmail(
            @RequestHeader("X-Internal-Api-Key") String internalApiKey,
            @RequestBody ResolveEmailRequest request);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ApiResponse<T>(
            boolean success, String code, String message, T data, Instant timestamp) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CurrentUserResponse(UUID id) {}

    record BatchProfileRequest(List<UUID> userIds) {}

    record ResolveEmailRequest(String email) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record InternalUserProfileResponse(
            UUID lookupUserId,
            UUID userId,
            String fullName,
            String email,
            String phoneNumber,
            String avatarUrl) {}
}
