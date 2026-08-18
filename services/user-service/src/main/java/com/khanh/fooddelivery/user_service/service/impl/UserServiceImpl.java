package com.khanh.fooddelivery.user_service.service.impl;

import com.khanh.fooddelivery.user_service.dto.request.UserProfileUpdateRequest;
import com.khanh.fooddelivery.user_service.dto.response.CurrentUserResponse;
import com.khanh.fooddelivery.user_service.entity.User;
import com.khanh.fooddelivery.user_service.exception.AppException;
import com.khanh.fooddelivery.user_service.exception.ErrorCode;
import com.khanh.fooddelivery.user_service.mapper.UserMapper;
import com.khanh.fooddelivery.user_service.repository.UserRepository;
import com.khanh.fooddelivery.user_service.service.UserService;
import com.khanh.fooddelivery.user_service.storage.AvatarStorageService;
import com.khanh.fooddelivery.user_service.storage.AvatarUploadResult;
import com.khanh.fooddelivery.user_service.util.JwtClaimUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JwtClaimUtils jwtClaimUtils;
    private final AvatarStorageService avatarStorageService;

    @Override
    public CurrentUserResponse getCurrentUser(Jwt jwt) {
        return userMapper.toResponse(getOrCreateUser(jwt));
    }

    @Override
    public CurrentUserResponse updateCurrentUser(
            Jwt jwt,
            UserProfileUpdateRequest request
    ) {
        User user = getOrCreateUser(jwt);
        String normalizedPhone = trimToNull(request.phoneNumber());

        if (request.phoneNumber() != null
                && normalizedPhone != null
                && !Objects.equals(normalizedPhone, user.getPhoneNumber())
                && userRepository.existsByPhoneNumberAndIdNot(
                        normalizedPhone,
                        user.getId()
                )) {
            throw new AppException(
                    ErrorCode.PHONE_NUMBER_ALREADY_EXISTS
            );
        }

        userMapper.updateEntity(request, user);
        normalizeUpdatedProfile(request, user);
        userRepository.flush();
        return userMapper.toResponse(user);
    }

    @Override
    public CurrentUserResponse updateAvatar(Jwt jwt, MultipartFile file) {
        User user = getOrCreateUser(jwt);
        AvatarUploadResult uploaded = avatarStorageService.upload(file, user.getId().toString());
        String previousStorageKey = user.getAvatarStorageKey();
        user.setAvatarUrl(uploaded.secureUrl());
        user.setAvatarStorageKey(uploaded.storageKey());
        userRepository.flush();
        deletePreviousAvatar(previousStorageKey);
        return userMapper.toResponse(user);
    }

    @Override
    public CurrentUserResponse deleteAvatar(Jwt jwt) {
        User user = getOrCreateUser(jwt);
        String previousStorageKey = user.getAvatarStorageKey();
        user.setAvatarUrl(null);
        user.setAvatarStorageKey(null);
        userRepository.flush();
        deletePreviousAvatar(previousStorageKey);
        return userMapper.toResponse(user);
    }

    private User getOrCreateUser(Jwt jwt) {
        String keycloakUserId = jwtClaimUtils.getSubject(jwt);
        User user = userRepository.findByKeycloakUserId(keycloakUserId)
                .orElseGet(() -> createUserFromJwt(jwt, keycloakUserId));
        synchronizeIdentityClaims(user, jwt);
        return user;
    }

    private User createUserFromJwt(Jwt jwt, String keycloakUserId) {
        String email = jwtClaimUtils.getEmail(jwt);
        String phoneNumber = jwtClaimUtils.getPhoneNumber(jwt);

        userRepository.insertProfileIfAbsent(
                keycloakUserId,
                jwtClaimUtils.getUsername(jwt),
                email,
                phoneNumber,
                jwtClaimUtils.getFullName(jwt),
                keycloakUserId
        );

        return userRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> creationConflict(email, phoneNumber));
    }

    private AppException creationConflict(
            String email,
            String phoneNumber
    ) {
        if (email != null && userRepository.existsByEmail(email)) {
            return new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (phoneNumber != null
                && userRepository.existsByPhoneNumber(phoneNumber)) {
            return new AppException(
                    ErrorCode.PHONE_NUMBER_ALREADY_EXISTS
            );
        }
        return new AppException(
                ErrorCode.DATA_CONFLICT,
                "Unable to create the user profile"
        );
    }

    private void synchronizeIdentityClaims(User user, Jwt jwt) {
        String username = jwtClaimUtils.getUsername(jwt);
        if (username != null
                && !Objects.equals(username, user.getUsername())) {
            user.setUsername(username);
        }

        String email = jwtClaimUtils.getEmail(jwt);
        if (email != null
                && !Objects.equals(email, user.getEmail())
                && !userRepository.existsByEmailAndIdNot(
                        email,
                        user.getId()
                )) {
            user.setEmail(email);
        }
    }

    private void normalizeUpdatedProfile(
            UserProfileUpdateRequest request,
            User user
    ) {
        if (request.fullName() != null) {
            user.setFullName(trimToNull(request.fullName()));
        }
        if (request.phoneNumber() != null) {
            user.setPhoneNumber(trimToNull(request.phoneNumber()));
        }
    }

    private void deletePreviousAvatar(String storageKey) {
        if (storageKey != null && !storageKey.isBlank()) {
            avatarStorageService.delete(storageKey);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
