package com.khanh.fooddelivery.user_service.controller;

import com.khanh.fooddelivery.user_service.common.response.ApiResponse;
import com.khanh.fooddelivery.user_service.config.InternalApiProperties;
import com.khanh.fooddelivery.user_service.dto.response.internal.InternalUserProfileResponse;
import com.khanh.fooddelivery.user_service.exception.AppException;
import com.khanh.fooddelivery.user_service.exception.ErrorCode;
import com.khanh.fooddelivery.user_service.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/users")
@RequiredArgsConstructor
public class InternalUserProfileController {
    private final UserRepository users;
    private final InternalApiProperties internalApi;

    @PostMapping("/batch-profile")
    public ApiResponse<List<InternalUserProfileResponse>> batchProfile(
            @RequestHeader(name = InternalSystemRoleController.INTERNAL_API_KEY_HEADER, required = false)
                    String apiKey,
            @RequestBody BatchProfileRequest request) {
        authenticate(apiKey);
        Collection<UUID> ids = request.userIds() == null ? List.of() : request.userIds().stream().distinct().toList();
        Map<UUID, com.khanh.fooddelivery.user_service.entity.User> usersById = users
                .findAllByIdOrKeycloakUserIdIn(
                        ids, ids.stream().map(UUID::toString).toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        com.khanh.fooddelivery.user_service.entity.User::getId,
                        Function.identity()));
        Map<String, com.khanh.fooddelivery.user_service.entity.User> usersByKeycloakId = usersById
                .values()
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        com.khanh.fooddelivery.user_service.entity.User::getKeycloakUserId,
                        Function.identity()));
        return ApiResponse.success(ids.stream()
                .map(id -> profile(id, usersById.get(id) != null ? usersById.get(id) : usersByKeycloakId.get(id.toString())))
                .filter(java.util.Objects::nonNull)
                .toList());
    }

    @PostMapping("/resolve-email")
    public ApiResponse<InternalUserProfileResponse> resolveByEmail(
            @RequestHeader(name = InternalSystemRoleController.INTERNAL_API_KEY_HEADER, required = false)
                    String apiKey,
            @Valid @RequestBody ResolveEmailRequest request) {
        authenticate(apiKey);
        return ApiResponse.success(users.findByEmailIgnoreCase(request.email().trim())
                .map(user -> profile(user.getId(), user))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND)));
    }

    private InternalUserProfileResponse profile(
            UUID lookupUserId,
            com.khanh.fooddelivery.user_service.entity.User user) {
        if (user == null) return null;
        return new InternalUserProfileResponse(
                lookupUserId,
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getAvatarUrl());
    }

    private void authenticate(String apiKey) {
        byte[] expected = internalApi.getKey().getBytes(StandardCharsets.UTF_8);
        byte[] supplied = (apiKey == null ? "" : apiKey).getBytes(StandardCharsets.UTF_8);
        if (expected.length == 0 || !MessageDigest.isEqual(expected, supplied)) throw new AppException(ErrorCode.ACCESS_DENIED);
    }

    public record BatchProfileRequest(List<UUID> userIds) {}

    public record ResolveEmailRequest(@NotBlank @Email String email) {}
}
