package com.khanh.fooddelivery.user_service.service;

import com.khanh.fooddelivery.user_service.dto.request.UserProfileUpdateRequest;
import com.khanh.fooddelivery.user_service.dto.response.CurrentUserResponse;
import org.springframework.security.oauth2.jwt.Jwt;

public interface UserService {

    CurrentUserResponse getCurrentUser(Jwt jwt);

    CurrentUserResponse updateCurrentUser(
            Jwt jwt,
            UserProfileUpdateRequest request
    );
}
