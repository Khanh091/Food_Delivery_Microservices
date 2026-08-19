package com.khanh.fooddelivery.user_service.dto.response.internal;

import java.util.UUID;

public record InternalUserProfileResponse(
        UUID lookupUserId,
        UUID userId,
        String fullName,
        String email,
        String phoneNumber,
        String avatarUrl) {}
