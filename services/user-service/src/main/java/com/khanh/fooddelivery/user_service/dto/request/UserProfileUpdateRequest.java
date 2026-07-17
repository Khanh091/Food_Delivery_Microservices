package com.khanh.fooddelivery.user_service.dto.request;

import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
        @Size(max = 255) String fullName,
        @Size(max = 20) String phoneNumber,
        @Size(max = 1000) String avatarUrl
) {
}
