package com.khanh.fooddelivery.user_service.controller;

import com.khanh.fooddelivery.user_service.common.response.ApiResponse;
import com.khanh.fooddelivery.user_service.dto.request.UserProfileUpdateRequest;
import com.khanh.fooddelivery.user_service.dto.response.CurrentUserResponse;
import com.khanh.fooddelivery.user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> getCurrentUser(
            @AuthenticationPrincipal Jwt jwt
    ) {
        CurrentUserResponse response =
                userService.getCurrentUser(jwt);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<CurrentUserResponse>>
    updateCurrentUser(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UserProfileUpdateRequest request
    ) {
        CurrentUserResponse response =
                userService.updateCurrentUser(jwt, request);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "User profile updated successfully",
                        response
                )
        );
    }
}
