package com.khanh.fooddelivery.user_service.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.user_service.config.InternalApiProperties;
import com.khanh.fooddelivery.user_service.dto.response.internal.InternalUserProfileResponse;
import com.khanh.fooddelivery.user_service.entity.User;
import com.khanh.fooddelivery.user_service.exception.AppException;
import com.khanh.fooddelivery.user_service.exception.ErrorCode;
import com.khanh.fooddelivery.user_service.repository.UserRepository;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InternalUserProfileControllerTests {
    @Mock private UserRepository users;
    @Captor private ArgumentCaptor<Collection<UUID>> ids;

    private InternalUserProfileController controller;
    private UUID userId;

    @BeforeEach
    void setUp() {
        InternalApiProperties properties = new InternalApiProperties();
        properties.setKey("internal-key");
        controller = new InternalUserProfileController(users, properties);
        userId = UUID.randomUUID();
    }

    @Test
    void batchProfileReturnsMinimalProfilesAndDeduplicatesIds() {
        UUID unknownUserId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setFullName("Nguyen Van A");
        user.setEmail("a@example.com");
        user.setPhoneNumber("0900000000");
        user.setAvatarUrl("https://cdn.example/avatar.png");
        when(users.findAllByIdOrKeycloakUserIdIn(any(), any())).thenReturn(List.of(user));

        List<InternalUserProfileResponse> profiles =
                controller
                        .batchProfile(
                                "internal-key",
                                new InternalUserProfileController.BatchProfileRequest(
                                        List.of(userId, userId, unknownUserId)))
                        .data();

        assertThat(profiles)
                .containsExactly(
                        new InternalUserProfileResponse(
                                userId,
                                userId,
                                "Nguyen Van A",
                                "a@example.com",
                                "0900000000",
                                "https://cdn.example/avatar.png"));
        verify(users).findAllByIdOrKeycloakUserIdIn(ids.capture(), any());
        List<UUID> capturedIds = List.copyOf(ids.getValue());
        assertThat(capturedIds).containsExactlyInAnyOrder(userId, unknownUserId);
    }

    @Test
    void batchProfileRejectsMissingOrInvalidInternalKey() {
        InternalUserProfileController.BatchProfileRequest request =
                new InternalUserProfileController.BatchProfileRequest(List.of(userId));

        assertThatThrownBy(() -> controller.batchProfile(null, request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
        assertThatThrownBy(() -> controller.batchProfile("wrong-key", request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }
}
