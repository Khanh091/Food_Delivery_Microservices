package com.khanh.fooddelivery.user_service.dto.response;

import com.khanh.fooddelivery.user_service.enums.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record CurrentUserResponse(
        UUID id,
        String keycloakUserId,
        String username,
        String email,
        String phoneNumber,
        String fullName,
        String avatarUrl,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
